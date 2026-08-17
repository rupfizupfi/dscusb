package ch.rupfizupfi.dscusb.dscusb

import jnr.ffi.annotations.In
import jnr.ffi.annotations.Out

/**
 * Mirrors the `DSCUSBDrv64` exports 1:1. Signatures follow the C typedefs in
 * `doc/dscusb/DSCUSBDrvXX-Documentation.pdf`, which are the authority here - the prose in the same
 * document writes `long` where the typedef says `uint32_t`, because it is describing the VB6 view
 * where `Long` is 32 bits. Map from the typedefs, never from the prose.
 *
 * Every export is `__stdcall`, which is a no-op on x86-64 - there is only one calling convention -
 * so no annotation is needed for the 64-bit DLL.
 */
interface DSCUSBDrv64 {
    fun OPENPORT(): Int
    fun CLOSEPORT()
    fun READCOMMAND(@In Serial: Int, @In Command: String, @Out Result: FloatArray): Int
    fun WRITECOMMAND(@In Serial: Int, @In Command: String, @In Value: Float): Int
    fun EXECUTECOMMAND(@In Serial: Int, @In Command: String): Int
    fun SETTIMEOUT(@In NewTimeout: Int): Int

    /** `uint32_t`, not 64-bit - see [DSCUSB.getTimeout] for the unsigned widening. */
    fun GETTIMEOUT(): Int
    fun VERSION(): Float
    fun DEVICECOUNT(): Int
    fun SERIALNUMBER(@In Index: Byte): Int

    /** Absent from the documented typedefs, but exported and used by the vendor's own C# sample. */
    fun REPORT(): Int
}
