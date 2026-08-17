package ch.rupfizupfi.dscusb.t24

import jnr.ffi.annotations.Delegate

/**
 * Data-provider packet callback. Invoked on a driver thread as packets arrive - see
 * `doc/t24/README.md` for the rules that apply to the body.
 *
 * [dataTag] is 32-bit. The vendor documents these parameters in VB terms, where `Long` means a
 * 32-bit integer, so it maps to Kotlin's [Int] and not [Long]; every T24 function that carries a
 * data tag (`PAIR`, `SOFTPAIR`, `SENDDATAPROVIDER`, `DATAPROVIDERINFO`) is 32-bit for the same
 * reason. Declaring it as [Long] made jnr-ffi build a function pointer expecting a 64-bit argument,
 * so the tag arrived with garbage in its upper half and never compared equal to the one `PAIR`
 * reported.
 */
interface DataCallback {
    @Delegate
    fun call(baseStation: Byte, dataTag: Int, value: Float, status: Byte, error: Byte, lowBatt: Byte, rssi: Byte, cv: Byte): Int
}
