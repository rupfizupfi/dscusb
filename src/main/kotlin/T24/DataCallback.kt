package ch.rupfizupfi.dscusb.T24

import jnr.ffi.annotations.Delegate

interface DataCallback {
    @Delegate
    fun call(baseStation: Byte, dataTag: Long, value: Float, status: Byte, error: Byte, lowBatt: Byte, rssi: Byte, cv: Byte): Int
}