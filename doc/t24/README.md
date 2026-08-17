# T24 (wireless)

Reference for the wireless backend, which wraps `T24AdvDrv64.dll` — Mantracourt's T24 Advanced Driver.
Code-level docs are in [../api.md](../api.md).

> **No vendor manual is bundled for this DLL,** and it is not on the public web — Mantracourt ships
> the API documentation and example code inside the
> [T24AdvDrv driver installer](https://www.mantracourt.com/wp-content/uploads/2024/07/t24advdrvxx_dll_driver_setup.zip)
> (Inno Setup, `T24AdvDrvXX DLL Driver 3.1`), whose payload cannot be read without running it. So the
> binding in `T24AdvDrv64.kt` was written against the exported symbol names and signatures.
>
> **If you install the driver, add its documentation to this directory** and link it from
> [../README.md](../README.md) — and re-check the [type widths](#parameter-widths) below, which are
> currently derived rather than quoted.

## The model

The wired backend is a straight request/response: you ask for `SYS`, you get a number. T24 is not, and
the difference drives the whole API shape.

- A **base station** plugs into the PC and owns the radio. `OPENPORT(comPort, baudrate)` opens it.
  Base stations are addressed by index (`0` for the first).
- **Transmitters** are battery-powered and spend most of their life asleep. They are not polled; they
  *push* packets when awake.
- **Pairing** binds one transmitter to the base station and yields two identifiers: an `ID` (the
  transmitter) and a `dataTag` (the data channel its readings arrive on).
- A **data provider** is the driver-side cache of the last packet seen for a `dataTag`.

So there are two ways to get readings, and they are not equivalent:

| Approach | Call | Semantics |
|---|---|---|
| Push | `registerDataCallback` | The driver invokes you as packets arrive. Real-time, on a driver thread. |
| Pull | `readLoadCellValue()` / `getDataProviderInfo()` | Reads the driver's cache. Returns immediately, may be stale. |

`T24Connection.readLoadCellValue()` is the pull form. It never solicits a fresh reading — if the
transmitter is asleep it hands back whatever arrived last, with no indication of age. Use
`getDataProviderInfo()` and check `LastReceived` when freshness matters.

## Power management

Transmitters sleep to save battery, so nothing arrives until you wake them:

| Call | Effect |
|---|---|
| `wakeAll()` | Broadcast wake to all transmitters on the latched base station |
| `sleepAll()` | Broadcast sleep |
| `stayAwakeModeOn/Off()` | Keep transmitters awake (driver-wide) |
| `alwaysWakeModeOn/Off(baseStation)` | Per-base-station variant |
| `commandSleep` / `commandResume` / `commandReset` / `commandSave` | Address one transmitter by `ID` |

The usual sequence is `open()` → `pair(...)` → register callbacks → `wakeAll()` → …work… →
`sleepAll()` → `close()`. Leaving transmitters awake drains their batteries, so `sleepAll()` is not
optional politeness.

## Pairing

```kotlin
val (id, dataTag) = connection.pair(baseStation = 0, useRemoteSettings = 0, configMode = 0, duration = 0)
```

`pair` blocks in the driver for up to `duration` and returns `(id, dataTag)`, latching both plus the
base station into the connection. Every subsequent call operates on that latched pair — **one
transmitter per `T24Connection`**. For several transmitters, construct several connections, or use
`T24WIFI` directly and track the tags yourself.

`softPair(baseStation, id, dataTag)` skips discovery when you already know both identifiers, which is
what you want in production once the hardware is fixed. It is much faster and needs no transmitter
interaction.

`pairAsync` starts `PAIRASYNC` then spins on `PAIRASYNCPOLL` until the `99` sentinel clears. Despite
the name it blocks the calling thread, with no sleep between polls and no timeout — a transmitter that
never shows up spins forever burning a core. Prefer `pair` with a `duration`, or drive
`T24WIFI.pairAsyncPoll` yourself if you need real non-blocking behaviour.

## Callbacks

Four slots exist in the driver; this wrapper surfaces two.

| Slot | Registered via | Fires on |
|---|---|---|
| `DP` | `registerDataCallback(DataCallback)` | Data-provider packets — readings |
| `WR` | `registerWakeUpCallback(WakeUpCallback)` | Wake-request packets |
| `SA` | `T24WIFI.registerCallbackSA(0)` | Spectrum analyser — not usefully exposed |
| `SAf` | `T24WIFI.registerCallbackSAF(0)` | T24-SAf fast transmitter packets — not usefully exposed |

The `SA`/`SAf` wrappers that take a lambda ignore it and pass `0`, i.e. they *clear* the slot rather
than installing anything. Do not mistake them for working registrations.

Three rules for callback bodies, all of which crash the JVM rather than throwing if broken:

1. **They run on a driver thread.** Hand values off through something thread-safe
   (`ConcurrentLinkedQueue`, as `CellValueStream` does) and return quickly. Blocking here stalls packet
   handling.
2. **Keep a strong reference to the registered instance** for as long as it is installed. If it is
   garbage-collected while the driver still holds its function pointer, the next packet dereferences
   freed memory.
3. **Do not call back into the connection** from inside the callback.

`SETCROSSTHREADCALLBACK` (`T24WIFI.setCrossThreadCallback()`) exists for GUI toolkits that must
marshal callbacks onto a UI thread. It is not needed for JVM consumers doing their own handoff.

Clear the slots with `unregisterCallbacks()` before `close()`, so the driver cannot invoke into a
callback whose port has gone away.

## Parameter widths

Device IDs and data tags are **32-bit** — Kotlin `Int`, not `Long`.

No T24 document states this directly, since none is bundled, but the DSC USB documentation establishes
the convention Mantracourt writes in: it describes `GETTIMEOUT` as returning a `long` in prose while
its own C typedef says `uint32_t`, i.e. their `Long` is VB6's 32-bit `Long`
([details](../dscusb/README.md#native-types-read-the-typedefs-not-the-prose)). Every T24 *function* in
`T24AdvDrv64.kt` agrees — `PAIR`, `PAIRASYNCPOLL`, `SOFTPAIR`, `SENDDATAPROVIDER`, `DATAPROVIDERINFO`,
`BASESTATIONID` and all four `COMMAND*` calls take or return the tag and ID as 32-bit.

`DataCallback` and `WakeUpCallback` originally declared them as Kotlin `Long`, transliterating the
vendor's VB `Long` literally. That is a real ABI mismatch, not a harmless widening: jnr-ffi builds the
callback's function pointer from the declared signature, so a 64-bit parameter reads a register the
driver only half wrote and the tag arrives with garbage in its upper 32 bits. The visible symptom was
that a tag from the callback never compared equal to the one `PAIR` returned, making it impossible to
tell which transmitter a packet came from. Both are now `Int`.

Callback *addresses* are the exception — `REGISTERCALLBACKDP(iCallbackAddress: Long)` and friends take
a real pointer, which is 64-bit in `T24AdvDrv64`.

Worth re-verifying against the vendor documentation if you install the driver. `BASESTATIONREVISION`
returning `Float` and the `Byte` status returns are on the same derived footing.

## Packet fields

Both callbacks and `getDataProviderInfo()` expose the same radio metadata alongside the value:

| Field | Meaning |
|---|---|
| `value` | The reading |
| `status` | Device status bits |
| `error` | Error bits |
| `lowBatt` | Transmitter battery is low — replace before it drops out mid-measurement |
| `rssi` | Received signal strength; weak signal means dropped packets, not wrong values |
| `cv` | Channel/config value |
| `msInterval` | Observed interval between packets (`getDataProviderInfo()` only) |
| `msLastReceived` | Age of the cached value (`getDataProviderInfo()` only) |

Because packets are pushed and radio is lossy, gaps are normal. A wireless stream is not a uniform
time series — use the `Measurement` timestamps or `msInterval` rather than assuming a fixed rate.

## Base stations

`getBaseStationInfo(baseStation)` returns `GroupKey`, `RFChannel`, `ID` and `FirmwareRevision`, and
throws if `BASESTATIONEXISTS` reports nothing there. Worth calling first when `open()` succeeds but no
packets ever arrive — it confirms the radio hardware is actually present and tells you which RF channel
it is on. Transmitters only talk to a base station on a matching channel;
`T24WIFI.baseStationChannelSet` changes it.

## Choosing a layer

`T24CellValueStream` handles the whole sequence above — open, pair, register, wake, and the mirrored
teardown — and hands you `Measurement`s through the same poll-and-drain interface as the wired stream.
Reach for it unless you need something it does not model:

| Need | Use |
|---|---|
| A stream of readings from one transmitter | `T24CellValueStream` |
| Several transmitters on one base station | `T24Connection` + your own callback, filtering on `dataTag` |
| Wake-request packets | `T24Connection.registerWakeUpCallback` |
| Radio metadata per reading (`rssi`, `lowBatt`, `status`) | `T24Connection` + your own callback |
| Base station diagnostics, channel changes | `T24Connection` / `T24WIFI` |

The stream filters packets down to the `dataTag` it paired with, so other transmitters on the same base
station are ignored rather than interleaved. It still handles only one transmitter, because pairing
latches a single tag — for several, run a connection per transmitter, or one callback that switches on
the tag itself.

`rssi` and `lowBatt` are worth watching even when you only want values: both predict dropouts, and
neither is visible through `Measurement`. If they matter, run your own callback alongside — or instead
of — the stream.
