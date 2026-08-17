# Documentation

Start with the [project README](../README.md) for install and quick-start. This directory holds the
reference material: our own API docs, plus the vendor manuals the bindings were written against.

## Ours

| Page | What it covers |
|---|---|
| [native-libraries.md](native-libraries.md) | Getting `DSCUSBDrv64.dll` / `T24AdvDrv64.dll` where the JVM can find them. Read this first if loading fails. |
| [api.md](api.md) | Class-by-class reference for both backends, including threading and lifecycle rules. |
| [dscusb/README.md](dscusb/README.md) | Wired DSC USB: parameter mnemonics, `STAT` bits, driver return codes. |
| [t24/README.md](t24/README.md) | Wireless T24: pairing model, callbacks, base-station handling. |

## Vendor material

Mantracourt originals, unmodified. Kept in-tree because the DLL API is not documented online in a
stable location, and the binding in `DSCUSBDrv64.kt` / `T24AdvDrv64.kt` must match it exactly.

| File | What it is |
|---|---|
| [dscusb/DSCUSBDrvXX-Documentation.pdf](dscusb/DSCUSBDrvXX-Documentation.pdf) ([html](dscusb/DSCUSBDrvXX-Documentation.html)) | The `DSCUSBDrv` DLL API: exported functions, arguments, return codes. This is what the `jnr-ffi` interface mirrors. |
| [dscusb/dscusb-advanced-manual.pdf](dscusb/dscusb-advanced-manual.pdf) ([html](dscusb/dscusb-advanced-manual.html)) | The *device* manual: every parameter mnemonic (`SYS`, `RATE`, `FLAG`, …), calibration, status and error bits. |
| [dscusb/csharp-example/Form1.cs](dscusb/csharp-example/Form1.cs) | Mantracourt's C# WinForms sample. Useful as a second opinion on `DllImport` signatures when a binding misbehaves. |

Original filenames were `DSCUSBDrvXX Documentation.pdf`, `dscusbadvancedmanual.pdf` and `Form1.cs`;
only the spaces were normalised.

No vendor manual for `T24AdvDrv64.dll` is included — [t24/README.md](t24/README.md) documents what the
binding was derived from.
