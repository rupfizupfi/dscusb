# API reference

Both backends use the same four layers. Understanding which layer you are on tells you what error
handling and threading to expect:

| Layer | Wired | Wireless | Responsibility |
|---|---|---|---|
| Binding | `DSCUSBDrv64` | `T24AdvDrv64` | `jnr-ffi` interface mirroring the DLL exports 1:1. Raw return codes, out-parameters as arrays. No checking. |
| Wrapper | `DSCUSB` | `T24WIFI` | Idiomatic names, unwraps out-parameters, converts return codes to exceptions. |
| Connection | `Connection` | `T24Connection` | Owns the port. Device discovery / pairing. Stateful. |
| Stream | `CellValueStream` | `T24CellValueStream` | Background reader; poll for accumulated `Measurement`s. |

Prefer the highest layer that does what you need. Drop to `DSCUSB`/`T24WIFI` for parameters the
connection does not expose; drop to the binding only when adding a new native function.

## Shared types

### `Measurement`

Immutable force reading plus the wall-clock time the wrapper received it.

```kotlin
class Measurement(force: Float, timestamp: Long)
    fun getForce(): Float      // device units, as configured on the cell
    fun getTimestamp(): Long   // System.currentTimeMillis() at read time
```

The timestamp is stamped by the JVM when the value comes back from the DLL, not by the device. It is
good enough to order samples and estimate rate, but it includes driver and scheduling latency — do
not treat it as a hardware capture time.

`getForce()` returns whatever scaling the cell is calibrated for; the wrapper applies none. `SYS` is
the calibrated system output, so units follow the device's `SGAI`/`SOFS` configuration.

### `CommandExecutionException`

```kotlin
class CommandExecutionException(val errorCode: Int) : Exception()
    override val message: String        // decoded from errorCode
    companion object { const val NON_NUMERIC_VALUE = -800 }
```

Thrown for any non-zero driver return code. `errorCode` is the raw value — branch on it rather than
on `message`. `NON_NUMERIC_VALUE` is ours, not the driver's: see
[dscusb/README.md](dscusb/README.md#driver-return-codes).

## Wired DSC USB

### `DSCUSB`

Return-code-checking wrapper. Constructing it loads `DSCUSBDrv64`.

```kotlin
fun openPort(): Int                    // 1 on success; does NOT throw, check the value
fun closePort()
fun readCommand(serial: Int, command: String): Float
fun writeCommand(serial: Int, command: String, value: Float)
fun executeCommand(serial: Int, command: String)
fun setTimeout(newTimeout: Int): Int
fun getTimeout(): Long                 // milliseconds; uint32_t widened unsigned
fun version(): Float
fun deviceCount(): Int
fun serialNumber(index: Byte): Int     // 1-based index
fun report(): Int                      // undocumented by the vendor, diagnostic only
```

`readCommand` is the one to be careful with. It throws on a non-zero return code, and additionally
throws `NON_NUMERIC_VALUE` when the driver reports success but leaves a non-finite float behind — the
out-parameter is seeded with `NaN` specifically so that case is detectable. Without that seeding a
zero-filled array would return `0.0f`, which is indistinguishable from a real "no load" reading.

`openPort()` breaks the pattern by returning a status instead of throwing; `1` means open. `Connection`
normalises this into an exception.

### `Connection`

Owns the port and latches the serial number of the first responding device.

```kotlin
fun open()                       // throws CommandExecutionException(-2) if no device answers
fun close()
fun getDeviceCount(): Int
fun getSerialNumber(): Int       // 0 until open() succeeds
fun readLoadCellValue(): Float   // SYS
fun readReadRateValue(): Byte    // RATE selector
fun readBaudRateValue(): Byte    // BAUD selector
fun getTimeout(): Long
```

`open()` enumerates `1..deviceCount()` and keeps the **first** non-zero serial number, so on a bench
with several conditioners attached you get an arbitrary one. Use `DSCUSB.serialNumber(index)` directly
if you need to choose.

`readReadRateValue()` and `readBaudRateValue()` return vendor selector codes, not Hz or baud — the
underlying `RATE`/`BAUD` parameters are `byte`-typed selectors. Resolve them against the advanced
manual.

### `CellValueStream`

Background reader. Owns its own `Connection` for its whole lifetime — do not open one yourself.

```kotlin
fun startReading()                    // opens the port, spawns the reader thread
fun stopReading()                     // asks the reader to stop; returns immediately
fun isReading(): Boolean
fun getLastError(): Throwable?
fun getNextValues(): List<Measurement>   // drains the queue
```

Lifecycle rules that matter:

- **`isReading()` is the real loop condition.** It goes false both when you call `stopReading()` and
  when the reader dies on a driver error. Polling `getNextValues()` without checking it means an
  unplugged cell looks identical to a quiet one.
- **`getLastError()` distinguishes the two.** Non-null means the reader ended on that throwable; null
  means it stopped on request or is still running.
- **A stream that ended cannot be restarted.** The reader closes the connection on its way out;
  construct a new `CellValueStream`.
- **The reader loop does not sleep.** It reads as fast as the driver allows and queues everything, so
  the queue grows without bound if you stop draining it. Poll on an interval that matches the device's
  configured `RATE`.
- `startReading()` and `stopReading()` throw `IllegalStateException` if another of the two is
  mid-flight, or if you start an already-running stream. `stopReading()` on an already-stopped stream
  is safe, so it works in a `finally` block regardless of how the reader ended.

Errors are handled inside the thread because there is no caller left to propagate to: every exit path
clears `running`, closes the connection, and parks the cause in `lastError`.

## Wireless T24

### `T24WIFI`

Wrapper over `T24AdvDrv64`, one method per export with normalised casing. Constructing it loads the
DLL. It does not check return codes — the T24 driver signals status with a `Byte` per call, and the
meaning varies by function, so interpretation is left to `T24Connection`.

Callback registration is overloaded: pass a `DataCallback`/`WakeUpCallback` to install one, or the
`Long` address form with `0` to clear it.

### `T24Connection`

```kotlin
fun open()                               // throws if the base station does not answer
fun close()
fun pair(baseStation: Byte, useRemoteSettings: Byte, configMode: Byte, duration: Byte): Pair<Int, Int>
fun pairAsync(...): Pair<Int, Int>       // blocks, polling until pairing settles
fun softPair(baseStation: Byte, id: Int, dataTag: Int): Byte
fun registerDataCallback(callback: DataCallback)
fun registerWakeUpCallback(callback: WakeUpCallback)
fun unregisterCallbacks()
fun wakeAll()
fun sleepAll()
fun commandResume(): Byte
fun getBaseStationInfo(baseStation: Byte): Map<String, Any>
fun getDataProviderInfo(): Map<String, Any>
fun readLoadCellValue(): Float
```

`pair` returns `(id, dataTag)` and latches both, plus the base station, into the connection. Everything
afterwards — `wakeAll()`, `commandResume()`, `readLoadCellValue()` — operates on that latched pair, so
a connection handles one transmitter at a time.

`pairAsync` is asynchronous only in the driver: it kicks off `PAIRASYNC` then spins on `PAIRASYNCPOLL`
until the sentinel `99` clears. It blocks the calling thread with no delay between polls and no
timeout, so a transmitter that never appears spins indefinitely. Prefer `pair` with a `duration`.

`readLoadCellValue()` reads the last cached data-provider value for the latched `dataTag`; it does not
solicit a fresh reading. A transmitter that has gone to sleep returns a stale value — check
`getDataProviderInfo()["LastReceived"]` and the `Status` bits if freshness matters.

`unregisterCallbacks()` clears all four slots including the two spectrum-analyser ones this wrapper
does not otherwise expose. Call it before `close()` so the driver cannot invoke into a callback whose
port is gone.

### `DataCallback` / `WakeUpCallback`

`@Delegate`-annotated interfaces `jnr-ffi` turns into native function pointers.

```kotlin
interface DataCallback {
    fun call(baseStation: Byte, dataTag: Int, value: Float, status: Byte,
             error: Byte, lowBatt: Byte, rssi: Byte, cv: Byte): Int
}

interface WakeUpCallback {
    fun call(baseStation: Byte, id: Int, rssi: Byte, cv: Byte): Int
}
```

`dataTag` and `id` are `Int` because the vendor's `Long` is VB's 32-bit one — declaring them as Kotlin
`Long` is an ABI mismatch that delivers a corrupted tag, not a wider one. See
[t24/README.md](t24/README.md#parameter-widths).

These are invoked **on a driver thread**, not yours. Keep the body short, hand values off through
something thread-safe, and do not call back into `T24Connection` from inside them. Return `0`.

Keep a strong reference to the instance you register for as long as it is installed — a callback that
gets collected while the driver still holds its pointer crashes the JVM rather than throwing.

### `T24CellValueStream`

Same contract as `CellValueStream` — start, poll, treat `isReading()` as the loop condition — over a
different mechanism.

```kotlin
class T24CellValueStream(
    baseStation: Byte = 0,
    useRemoteSettings: Byte = 0,
    configMode: Byte = 0,
    pairingDuration: Byte = 5,
)
    fun startReading()                       // opens, pairs, registers the callback, wakes
    fun stopReading()
    fun isReading(): Boolean
    fun getLastError(): Throwable?
    fun getPairedDevice(): Pair<Int, Int>?   // (id, dataTag), null until pairing succeeds
    fun getNextValues(): List<Measurement>
```

The wired stream *pulls*: its thread calls the driver in a loop. This one cannot, because T24
transmitters *push*. The reader thread only owns the lifecycle — pair, register, wake, then wait for a
stop request — while packets arrive on a driver thread and are queued by the callback. Consequences:

- **Readings appear at the transmitter's pace, with gaps** whenever the radio drops a packet. Use the
  `Measurement` timestamps; there is no fixed interval to assume.
- **Timestamps are stamped on arrival at the JVM**, so they include radio and driver latency. They
  order samples correctly but are not hardware capture times.
- **Packets are filtered to the paired `dataTag`**, so other transmitters on the same base station are
  ignored. The filter fails open until pairing records a tag — an always-empty queue would be
  indistinguishable from a silent cell, which is the worse failure.

Lifecycle rules match `CellValueStream`: the reader traps its own errors and parks them in
`getLastError()`, `isReading()` goes false on both a requested stop and a failure, and a stream that
ended cannot be restarted. Teardown sleeps the transmitters, clears the callbacks, then closes the
port, each step guarded so one failure cannot skip the close.

Pairing parameters are constructor arguments rather than hardcoded, since they depend on the
installation. `pairingDuration` is the window the driver waits for a transmitter to answer.
