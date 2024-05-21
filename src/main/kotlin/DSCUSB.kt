package com.glowingblue.intellij

import jnr.ffi.LibraryLoader
import jnr.ffi.Platform

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
        var result = 0f
        library.READCOMMAND(serial, command, result)
        return result
    }

    fun writeCommand(serial: Int, command: String, value: Float): Int {
        return library.WRITECOMMAND(serial, command, value)
    }

    fun executeCommand(serial: Int, command: String): Int {
        return library.EXECUTECOMMAND(serial, command)
    }

    fun setTimeout(newTimeout: Int): Int {
        return library.SETTIMEOUT(newTimeout)
    }

    fun getTimeout(): Int {
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