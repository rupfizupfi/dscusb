package ch.rupfizupfi.dscusb.t24

import jnr.ffi.annotations.Delegate

/**
 * Wake-request packet callback. Invoked on a driver thread - see `doc/t24/README.md`.
 *
 * [id] is a 32-bit device ID, for the same reason [DataCallback.call]'s tag is: the vendor's `Long`
 * is VB's 32-bit `Long`. `BASESTATIONID`, `PAIR` and every `COMMAND*` function agree.
 */
interface WakeUpCallback {
    @Delegate
    fun call(baseStation: Byte, id: Int, rssi: Byte, cv: Byte): Int
}
