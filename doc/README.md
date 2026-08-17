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

Mantracourt's own documentation of the DLL API, which the bindings in `DSCUSBDrv64.kt` /
`T24AdvDrv64.kt` must match exactly. The HTML manuals are in-tree because the API is not documented
online in a stable location.

| File | In repo | What it is |
|---|---|---|
| [dscusb/DSCUSBDrvXX-Documentation.html](dscusb/DSCUSBDrvXX-Documentation.html) | yes | The `DSCUSBDrv` DLL API: exported functions, arguments, return codes, and the C typedefs the binding follows. |
| [dscusb/dscusb-advanced-manual.html](dscusb/dscusb-advanced-manual.html) | yes | The *device* manual: every parameter mnemonic (`SYS`, `RATE`, `FLAG`, …), calibration, status and error bits. |
| `dscusb/*.pdf` | **no** | PDF originals of both manuals. Gitignored — fetch from Mantracourt and drop them in `doc/dscusb/` if you want them locally. |
| `dscusb/csharp-example/Form1.cs` | **no** | Mantracourt's C# WinForms sample, useful as a second opinion on `DllImport` signatures. Gitignored; ships in the [DSCUSB driver package](https://www.mantracourt.com/resources/driver-dll-dsc-usb/). |

The PDFs and the C# sample are deliberately **not** committed, so this repo does not redistribute
vendor binaries or sample code. Everything the bindings actually depend on — the typedefs, the
parameter table, the `STAT` bits, the return codes — is transcribed into
[dscusb/README.md](dscusb/README.md), so the reference survives without them.

Original filenames were `DSCUSBDrvXX Documentation.html` and `dscusbadvancedmanual.html`; only the
spaces were normalised.

No vendor documentation for `T24AdvDrv64.dll` is available at all — [t24/README.md](t24/README.md)
documents what the binding was derived from instead.
