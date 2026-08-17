# Native libraries

This project is a thin binding: it ships no native code. Both backends load a vendor DLL at class
construction time, so a missing or mismatched DLL fails immediately and loudly.

| Class | Loads | Vendor package |
|---|---|---|
| `ch.rupfizupfi.dscusb.dscusb.DSCUSB` | `DSCUSBDrv64` | DSCUSB driver / toolkit |
| `ch.rupfizupfi.dscusb.t24.T24WIFI` | `T24AdvDrv64` | T24 Advanced Driver / Toolkit |

Both are obtained from [Mantracourt](https://www.mantracourt.com) — they are not redistributed here.

## Bit-ness must match

The names are not decoration: `DSCUSBDrv64.dll` and `T24AdvDrv64.dll` are the **64-bit** builds, and
they only load into a 64-bit JVM. The 32-bit variants (`DSCUSBDrv.dll`, `T24AdvDrv.dll`) will fail
even if the filename is right, because the loader matches architecture before anything else. Check
with:

```bash
java -XshowSettings:properties -version 2>&1 | grep os.arch    # expect amd64
```

## Where the loader looks

`LibraryLoader.create(...).load("DSCUSBDrv64")` hands the bare name to the platform loader, which on
Windows searches the executable directory, the system directories, and then everything on `PATH`.
Pick whichever of these fits how you run the code:

**1. A directory on `PATH`** — most convenient for development:

```powershell
$env:PATH = "C:\mantracourt\dll;$env:PATH"
./gradlew run --args="dscusb"
```

**2. `-Djava.library.path`** — explicit, and survives a shell that forgot its `PATH`:

```bash
./gradlew run --args="dscusb" -Dorg.gradle.jvmargs="-Djava.library.path=C:/mantracourt/dll"
```

**3. Next to the launched jar** — what the vendor's own examples assume, and what
Mantracourt's C# sample warns about in its load handler. Dropping the DLL beside
`dscusb.jar` works for a deployed fat jar.

Avoid copying DLLs into `C:\Windows\System32`. It works, and the vendor sample even suggests it, but
it makes the version you are actually running impossible to reason about later.

## Diagnosing failures

`UnsatisfiedLinkError` / `Could not load library` at the point you construct `DSCUSB()` or `T24WIFI()`
means the DLL was never found or never loaded. In order of likelihood:

1. The DLL is not on any searched path — confirm with `where DSCUSBDrv64.dll` in the same shell.
2. Architecture mismatch — 32-bit DLL, or a 32-bit JVM.
3. A missing dependency *of* the DLL. The driver needs the FTDI USB serial layer; if the device has
   never been plugged into this machine, install the FTDI D2XX/VCP drivers first. The DLL itself
   loads, then fails to initialise.

A DLL that loads but returns `-2` ("Cannot open or close serial port") from `openPort()` is a
different problem: the library is fine, but no device is attached, or another process already holds
the port. Only one process can own the USB interface at a time — close the vendor's *Instrument
Explorer* / toolkit before running this code.

## Timeouts

`DSCUSB.setTimeout(ms)` / `getTimeout()` map onto the DLL's own serial timeout, and it is global to
the loaded library rather than per-connection. Raising it makes `-100` ("No response") less likely on
a slow or noisy link, at the cost of blocking the reader thread for longer on a genuinely dead
device. The default is whatever the DLL ships with; read it back with `getTimeout()` rather than
assuming a value.
