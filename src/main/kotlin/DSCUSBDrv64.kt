package com.glowingblue.intellij

import jnr.ffi.annotations.In
import jnr.ffi.annotations.Out

interface DSCUSBDrv64 {
    fun OPENPORT(): Int
    fun CLOSEPORT()
    fun READCOMMAND(@In Serial: Int, @In Command: String, @Out Result: Float): Int
    fun WRITECOMMAND(@In Serial: Int, @In Command: String, @In Value: Float): Int
    fun EXECUTECOMMAND(@In Serial: Int, @In Command: String): Int
    fun SETTIMEOUT(@In NewTimeout: Int): Int
    fun GETTIMEOUT(): Int
    fun VERSION(): Float
    fun DEVICECOUNT(): Int
    fun SERIALNUMBER(@In Index: Byte): Int
    fun REPORT(): Int
}