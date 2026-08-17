# dscusb

JVM wrapper around the [Mantracourt](https://www.mantracourt.com) load-cell driver DLLs. It turns the
flat C API into Kotlin classes and a background reader you can poll from Java or Kotlin, so callers
never touch `jnr-ffi` or the native calling convention themselves.

Two backends are supported, each mirroring one vendor DLL:

| Backend | Native library | Hardware | Entry point |
|---|---|---|---|
| DSC USB (wired) | `DSCUSBDrv64.dll` | DSC / DSCUSB strain-gauge conditioner over USB | `ch.rupfizupfi.dscusb.dscusb.CellValueStream` |
| T24 (wireless) | `T24AdvDrv64.dll` | T24 telemetry base station + paired transmitters | `ch.rupfizupfi.dscusb.t24.T24Connection` |

> **Status:** `0.0.1-beta.1`. The DSC USB path is the exercised one; T24 support is newer and has had
> less time against real hardware. See [Known limitations](#known-limitations).

## Requirements

- **Windows x64.** Both DLLs are 64-bit and the driver talks to an FTDI USB serial device.
- **JDK 26** — the build requests a toolchain, so Gradle can provision it via the Foojay resolver.
- **The vendor DLL for your backend**, reachable on the library path. This is the single most common
  cause of a failed first run; see [doc/native-libraries.md](doc/native-libraries.md).

## Build

```bash
./gradlew build          # compile + test
./gradlew shadowJar      # fat jar -> build/libs/dscusb.jar
```

To smoke-test against real hardware:

```bash
./gradlew run --args="dscusb"   # wired cell
./gradlew run --args="t24"      # wireless base station
```

Both are implemented in [`examples/Demo.kt`](src/main/kotlin/ch/rupfizupfi/dscusb/examples/Demo.kt).

## Quick start

### Streaming a wired DSC USB cell

`CellValueStream` owns its own connection and reads on a background thread. You poll it for whatever
has accumulated since the last call, which keeps the native read loop off your thread.

```kotlin
import ch.rupfizupfi.dscusb.dscusb.CellValueStream

val stream = CellValueStream()
stream.startReading()

while (stream.isReading()) {
    stream.getNextValues().forEach { m ->
        println("${m.getForce()} N @ ${m.getTimestamp()}")
    }
    Thread.sleep(100)
}

// A reader that ended by itself parks the cause here; null means it was stopped on request.
stream.getLastError()?.let { throw it }
stream.stopReading()
```

`isReading()` goes false whether the reader was asked to stop *or* died on a driver error (unplugged
cell, DLL fault). Always check it in the loop condition — a stream that ended on its own cannot be
restarted, construct a new instance.

### One-off reads and raw parameters

`Connection` is the thin synchronous layer if you want single readings or device metadata:

```kotlin
import ch.rupfizupfi.dscusb.dscusb.Connection

val connection = Connection()
connection.open()                       // finds the first device and latches its serial number
try {
    println(connection.readLoadCellValue())   // the SYS parameter
    println(connection.getSerialNumber())
    println(connection.readReadRateValue())   // RATE
} finally {
    connection.close()
}
```

For parameters the wrapper does not expose, go through `DSCUSB` directly — `readCommand`,
`writeCommand` and `executeCommand` take any mnemonic from the
[parameter table](doc/dscusb/README.md#parameters).

### Wireless T24

```kotlin
import ch.rupfizupfi.dscusb.t24.T24Connection
import ch.rupfizupfi.dscusb.t24.DataCallback

val connection = T24Connection()
connection.open()
val (id, dataTag) = connection.pair(0, 0, 0, 0)

connection.registerDataCallback(object : DataCallback {
    override fun call(
        baseStation: Byte, dataTag: Long, value: Float, status: Byte,
        error: Byte, lowBatt: Byte, rssi: Byte, cv: Byte
    ): Int {
        println("$value (rssi=$rssi lowBatt=$lowBatt)")
        return 0
    }
})

connection.wakeAll()
// ... transmitters now push packets into the callback ...
connection.sleepAll()
connection.close()
```

If you just want readings rather than raw packets, `T24CellValueStream` wraps all of the above —
pairing, waking, callback registration and teardown — behind the same poll-and-drain interface as the
wired `CellValueStream`:

```kotlin
val stream = T24CellValueStream(baseStation = 0, pairingDuration = 5)
stream.startReading()
while (stream.isReading()) {
    stream.getNextValues().forEach(::println)
    Thread.sleep(100)
}
stream.getLastError()?.let { throw it }
```

Readings arrive at the transmitter's pace with gaps whenever the radio drops a packet, so use the
`Measurement` timestamps rather than assuming a fixed interval.

## Error handling

Every non-zero driver return code becomes a `CommandExecutionException` carrying the raw `errorCode`
and a decoded `message`. The wrapper also raises it for one case the DLL cannot signal: `readCommand`
seeds its out-parameter with `NaN`, so a "success" return that left a non-finite value behind is
rejected as `NON_NUMERIC_VALUE` (`-800`) rather than being handed back as a plausible-looking `0.0`
force reading. Codes are listed in [doc/dscusb/README.md](doc/dscusb/README.md#driver-return-codes).

## Project layout

```
src/main/kotlin/ch/rupfizupfi/dscusb/
  Measurement.kt                 force + timestamp value type, shared by both backends
  CommandExecutionException.kt   driver return code -> exception
  dscusb/                        wired DSC USB backend
    DSCUSBDrv64.kt               jnr-ffi binding, mirrors the DLL exports 1:1
    DSCUSB.kt                    return-code checking over the binding
    Connection.kt                port lifecycle + device discovery
    CellValueStream.kt           background reader, poll with getNextValues()
  t24/                           wireless T24 backend
    T24AdvDrv64.kt               jnr-ffi binding
    T24WIFI.kt                   naming/case normalisation over the binding
    T24Connection.kt             pairing, wake/sleep, callbacks
    T24CellValueStream.kt        background reader, poll with getNextValues()
    DataCallback.kt              @Delegate callback for data-provider packets
    WakeUpCallback.kt            @Delegate callback for wake-request packets
  examples/Demo.kt               hardware smoke test, `./gradlew run`

doc/                             see doc/README.md
```

The layering is the same on both sides: a `jnr-ffi` interface that mirrors the DLL exactly, a wrapper
that checks return codes, a connection that owns the port, and a stream that reads in the background.
Keep new native functions flowing through those layers rather than calling the binding directly.

## Documentation

- [doc/README.md](doc/README.md) — documentation index
- [doc/native-libraries.md](doc/native-libraries.md) — installing and locating the DLLs
- [doc/api.md](doc/api.md) — class-by-class API reference
- [doc/dscusb/](doc/dscusb/) — DSC USB parameters, return codes, vendor manuals
- [doc/t24/](doc/t24/) — T24 concepts and API surface

## Known limitations

- **The T24 binding's type widths are derived, not quoted.** No vendor documentation for
  `T24AdvDrv64.dll` is publicly available — it ships inside the driver installer. The signatures follow
  the convention the DSC USB documentation establishes plus internal consistency; see
  [doc/t24/README.md](doc/t24/README.md#parameter-widths). Re-verify if you install the driver.
- **One transmitter per `T24CellValueStream`.** Pairing latches a single data tag. Run a connection per
  transmitter, or one callback switching on the tag, for more.
- **No automated tests.** `src/test` is empty; everything meaningful requires attached hardware.
- **`REPORT()` is undocumented.** It is exported by the DLL and bound in `DSCUSBDrv64`, but does not
  appear in the vendor documentation. Treat its return value as diagnostic only.

## Third-party material

The HTML manuals under `doc/dscusb/` are Mantracourt material, included for reference and not covered
by this project's terms. Their PDF originals and Mantracourt's C# sample are **not** committed — they
are gitignored, so fetch them from the vendor if you want them locally. See
[doc/README.md](doc/README.md#vendor-material).
