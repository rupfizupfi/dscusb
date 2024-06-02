package ch.rupfizupfi.dscusb

import jnr.ffi.LibraryLoader

class DSCUSB {
    private val library: DSCUSBDrv64

    init {
        System.loadLibrary("DSCUSBDrv64")
        library = LibraryLoader.create(DSCUSBDrv64::class.java).load("DSCUSBDrv64")
    }

    fun openPort(): Int {
        return library.OPENPORT()
    }

    fun closePort() {
        library.CLOSEPORT()
    }

    fun readCommand(serial: Int, command: String): Float {
        var value = FloatArray(1)
        val result = library.READCOMMAND(serial, command, value)
        if (result != 0) {
            throw CommandExecutionException(result)
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
     */
    fun getTimeout(): Long {
        return library.GETTIMEOUT()
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