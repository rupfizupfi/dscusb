package ch.rupfizupfi.dscusb.T24

import jnr.ffi.annotations.Delegate

interface WakeUpCallback {
    @Delegate
    fun call(baseStation: Byte, id: Long, rssi: Byte, cv: Byte): Int
}