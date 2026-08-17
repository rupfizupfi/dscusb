# DSC USB (wired)

Reference for the wired backend. Code-level docs are in [../api.md](../api.md); this page covers the
device's own vocabulary — the parameter mnemonics you pass to `readCommand` / `writeCommand` /
`executeCommand`, and what the driver's return codes mean.

Source of truth for everything below:

- [DSCUSBDrvXX-Documentation.html](DSCUSBDrvXX-Documentation.html) — the DLL API
- [dscusb-advanced-manual.html](dscusb-advanced-manual.html) — the device parameters
- `csharp-example/Form1.cs` — vendor C# sample, not committed (see [../README.md](../README.md#vendor-material))

## Native types: read the typedefs, not the prose

The bundled documentation describes each function twice, and the two disagree. Take `GETTIMEOUT`:

> `long GETTIMEOUT()` — "Returns: A long value representing the current millisecond timeout setting"

> `typedef uint32_t (__stdcall* GETTIMEOUT)(void);`

Same function. The prose is written from the VB6 view, where `Long` is **32-bit**; the typedef is C.
The vendor's own C# sample settles it — `public static extern Int32 GetTimeout();`.

**So a Mantracourt `long` is a Kotlin `Int`, never a Kotlin `Long`.** Getting this wrong is not
cosmetic: jnr-ffi builds the native call frame from the declared signature, so a 64-bit declaration
against a 32-bit value reads a register the callee never fully wrote, and the upper half is garbage.
It bit this project twice — `GETTIMEOUT` and both T24 callbacks (see [../t24/README.md](../t24/README.md)).

These are the authoritative signatures, transcribed from the documentation:

```c
typedef uint32_t (__stdcall* OPENPORT)(void);
typedef void     (__stdcall* CLOSEPORT)(void);
typedef uint32_t (__stdcall* DEVICECOUNT)(void);
typedef uint32_t (__stdcall* SERIALNUMBER)(uint8_t Index);
typedef uint32_t (__stdcall* READCOMMAND)(uint32_t Serial, string Command, float* Result);
typedef uint32_t (__stdcall* WRITECOMMAND)(uint32_t Serial, string Command, float Value);
typedef uint32_t (__stdcall* EXECUTECOMMAND)(uint32_t Serial, string Command);
typedef uint32_t (__stdcall* SETTIMEOUT)(uint32_t NewTimeout);
typedef uint32_t (__stdcall* GETTIMEOUT)(void);
typedef float    (__stdcall* VERSION)(void);
```

Two gaps in that list. `REPORT` is not in it, but is exported and used by the vendor's C# sample as
`Int32 Report()` — `DSCUSBDrv64` binds it on that basis. And the prose gives `SETTIMEOUT` a `void`
return while the typedef and the C# sample both give it a 32-bit one; the binding follows the latter
two.

`__stdcall` needs no jnr-ffi annotation here: x86-64 has a single calling convention, so it is a no-op
for the 64-bit DLL. It would matter for `DSCUSBDrv32`.

## How commands work

The DLL exposes three parameter operations, all keyed by device serial number and a short mnemonic:

| Operation | Wrapper | Use for |
|---|---|---|
| `READCOMMAND` | `DSCUSB.readCommand(serial, "SYS")` | any `RO` or `RW` parameter |
| `WRITECOMMAND` | `DSCUSB.writeCommand(serial, "STN", 1f)` | `RW` parameters |
| `EXECUTECOMMAND` | `DSCUSB.executeCommand(serial, "SNAP")` | `X` (action) parameters, which take no value |

Values cross the boundary as `float` regardless of the parameter's logical type, so `int`/`byte`
parameters need converting on the way out — that is what `Connection.readReadRateValue()` does when it
narrows `RATE` to a `Byte`.

Writes are volatile. Persist them with `executeCommand(serial, "SAVE")` if they should survive a power
cycle; check the manual for which parameters are savable before relying on this.

## Parameters

Access column: `RO` read-only, `RW` read/write, `X` execute (no value).

### Readings

| Mnemonic | Meaning | Type | Access |
|---|---|---|---|
| `SYS` | Main output — the calibrated system value. This is what `readLoadCellValue()` reads. | float | RO |
| `CELL` | Cell output | float | RO |
| `SRAW` | Raw system output | float | RO |
| `CRAW` | Raw cell output | float | RO |
| `MVV` | Filtered & factory-calibrated mV/V | float | RO |
| `TEMP` | Temperature | float | RO |
| `SYSN` | Snapshot result — the value captured by `SNAP` | float | RO |
| `PEAK` | Peak value | float | RO |
| `TROF` | Trough value | float | RO |
| `STAT` | Live status bits — see [below](#stat-bits) | int | RO |
| `FLAG` | Latched error flags; reset by writing `0` | int | RW |

### Identity

| Mnemonic | Meaning | Type | Access |
|---|---|---|---|
| `VER` | Software version | byte | RO |
| `SERL` | Serial number, low word | int | RO |
| `SERH` | Serial number, high word | int | RO |
| `STN` | Station number | int | RW |

The DLL's `SERIALNUMBER(index)` already returns the combined serial, so `SERL`/`SERH` are only needed
if you are talking to the device outside this wrapper.

### Communication and formatting

| Mnemonic | Meaning | Type | Access |
|---|---|---|---|
| `BAUD` | Baud rate **selector code** | byte | RW |
| `RATE` | Reading rate **selector code** | byte | RW |
| `OPCL` | Output control (value select) | byte | RW |
| `DP` | Digits after the decimal point | byte | RW |
| `DPB` | Digits before the decimal point | byte | RW |

`BAUD` and `RATE` are enumerated selectors, not literal baud/Hz values — the manual lists the code
table. `Connection.readBaudRateValue()` / `readReadRateValue()` return these codes unmodified.

### Calibration and scaling

| Mnemonic | Meaning | Type | Access |
|---|---|---|---|
| `NMVV` | Nominal mV/V for scaling `ELEC` | float | RW |
| `CGAI` / `COFS` | Cell gain / offset | float | RW |
| `CMIN` / `CMAX` | Cell range min / max | float | RW |
| `SGAI` / `SOFS` | System gain / offset | float | RW |
| `SMIN` / `SMAX` | System range min / max | float | RW |
| `CLN` | Linearisation point count | byte | RW |
| `CLX1`..`CLX7` | Linearisation raw values | float | RW |
| `CLK1`..`CLK7` | Linearisation corrections | float | RW |
| `CTN` | Temperature-compensation point count | byte | RW |
| `CT1`..`CT5` | Tempco temperature points | float | RW |
| `CTG1`..`CTG5` | Tempco gain adjustments | float | RW |
| `CTO1`..`CTO5` | Tempco offset adjustments | float | RW |
| `FFLV` | Dynamic filter level | float | RW |
| `FFST` | Dynamic filter steps | float | RW |

Changing gain/offset/linearisation silently changes the meaning of every subsequent `SYS` reading. The
wrapper exposes no helpers for these deliberately — use the vendor toolkit to calibrate, and read the
manual's calibration chapter first.

### Actions

| Mnemonic | Effect |
|---|---|
| `RST` | Reboot the device |
| `SNAP` | Take a snapshot; read the result from `SYSN` |
| `RSPT` | Reset peak and trough |
| `SCON` / `SCOF` | Shunt calibration resistor on / off |
| `OPON` / `OPOF` | Digital output on / off |

## `STAT` bits

Live status. Some bits mirror `FLAG`, but `STAT` reflects the present moment while `FLAG` latches until
cleared.

| Bit | Value | Name | Meaning |
|---|---|---|---|
| 0 | 1 | `SPSTAT` | Setpoint output status |
| 1 | 2 | `IPSTAT` | Digital input status (DSC only) |
| 2 | 4 | `TEMPUR` | Temperature under range |
| 3 | 8 | `TEMPOR` | Temperature over range |
| 4 | 16 | `ECOMUR` | Strain gauge input under range |
| 5 | 32 | `ECOMOR` | Strain gauge input over range |
| 6 | 64 | `CRAWUR` | Cell under range |
| 7 | 128 | `CRAWOR` | Cell over range |
| 8 | 256 | `SYSUR` | System under range |
| 9 | 512 | `SYSOR` | System over range |
| 11 | 2048 | `LCINTEG` | Load cell integrity error |
| 12 | 4096 | `SCALON` | Shunt calibration resistor on |
| 13 | 8192 | `OLDVAL` | Stale output value (already read) |

Bits 10, 14 and 15 are reserved.

Two of these matter for anyone consuming `CellValueStream`. `OLDVAL` means you re-read a value the
device had already handed over — if it is set often, you are polling faster than the configured `RATE`.
`LCINTEG` means the load cell wiring itself is suspect, and readings should not be trusted regardless
of how plausible they look. The wrapper does not check `STAT`; read it yourself via
`DSCUSB.readCommand(serial, "STAT")` if your application needs that assurance.

## Driver return codes

Returned by every DLL function; `DSCUSB` turns anything non-zero into a `CommandExecutionException`.

| Code | Message | Notes |
|---|---|---|
| `0` | Received response OK | Vendor-documented |
| `-1` | Invalid argument value in function call | Vendor-documented. An argument is outside allowed limits. |
| `-2` | Cannot open or close serial port | No device attached, or another process owns the port |
| `-100` | No response | Vendor-documented. The instrument sent nothing back; consider `setTimeout`. |
| `-200` | Invalid response: there was a response, but the data could not be decoded | Vendor-documented |
| `-300` | Invalid checksum | |
| `-400` | Not acknowledged (NAK) | Vendor-documented. Illegal value written, or illegal command number. |
| `-500` | Invalid reply length | |
| `-600` | Stream buffer full | |
| `-700` | No stream data | |
| `-800` | Non-numeric value returned while the driver reported success | **Not a driver code** — see below |

The bundled `DSCUSBDrvXX-Documentation` revision only tabulates `0`, `-1`, `-100`, `-200` and `-400`.
The rest come from the driver's own header and other revisions of the documentation; they are handled
because the DLL does emit them.

`-200` previously rendered as "Invalid station number in response", which was too narrow — a bad
station number is only one of several ways a reply can fail to decode. It now carries the vendor
wording. Branch on `errorCode`, never on `message`.

### `-800` is ours

The DLL's contract is that the `float` out-parameter is meaningful whenever the return code is `0`.
`DSCUSB.readCommand` seeds that array with `NaN` before the call, so a `0` return that left a
non-finite value behind is detectable — and it is rejected as `NON_NUMERIC_VALUE` rather than returned.
A zero-filled array would instead have produced `0.0f`, which reads as a perfectly ordinary "no load"
measurement. For a force-measurement library, silently plausible garbage is worse than a loud failure.
