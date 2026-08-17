package ch.rupfizupfi.dscusb.dscusb

import ch.rupfizupfi.dscusb.CommandExecutionException
import jnr.ffi.LibraryLoader

class DSCUSB {
    private val library: DSCUSBDrv64

    init {
        library = LibraryLoader.create(DSCUSBDrv64::class.java).load("DSCUSBDrv64")
    }

    fun openPort(): Int {
        return library.OPENPORT()
    }

    fun closePort() {
        library.CLOSEPORT()
    }

    fun readCommand(serial: Int, command: String): Float {
        // Seeded with NaN rather than left at Kotlin's zero fill: if the DLL ever returns 0 without
        // writing the out-parameter, a zero-filled array would hand back 0.0f - indistinguishable
        // from a genuine "no load" reading, and far more dangerous than an obvious garbage value.
        val value = FloatArray(1) { Float.NaN }
        val result = library.READCOMMAND(serial, command, value)
        if (result != 0) {
            throw CommandExecutionException(result)
        }
        if (!value[0].isFinite()) {
            throw CommandExecutionException(CommandExecutionException.NON_NUMERIC_VALUE)
        }
        return value[0]
    }

    fun writeCommand(serial: Int, command: String, value: Float) {
        val result = library.WRITECOMMAND(serial, command, value)
        if (result != 0) {
            throw CommandExecutionException(result)
        }
    }

    fun executeCommand(serial: Int, command: String) {
        val result = library.EXECUTECOMMAND(serial, command)
        if (result != 0) {
            throw CommandExecutionException(result)
        }
    }

    fun setTimeout(newTimeout: Int): Int {
        return library.SETTIMEOUT(newTimeout)
    }

    /**
     * Get the timeout in milliseconds.
     *
     * The DLL returns `uint32_t`. Widened here rather than exposed as a raw [Int] so the unsigned
     * range survives: a timeout above 2^31 ms would otherwise surface as a negative number.
     */
    fun getTimeout(): Long {
        return library.GETTIMEOUT().toLong() and 0xFFFFFFFFL
    }

    fun version(): Float {
        return library.VERSION()
    }

    fun deviceCount(): Int {
        return library.DEVICECOUNT()
    }

    fun serialNumber(index: Byte): Int {
        return library.SERIALNUMBER(index)
    }

    fun report(): Int {
        return library.REPORT()
    }
}