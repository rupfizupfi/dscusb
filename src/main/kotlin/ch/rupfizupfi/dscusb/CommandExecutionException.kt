package ch.rupfizupfi.dscusb

class CommandExecutionException(val errorCode: Int) : Exception() {
    companion object {
        /**
         * Not a driver code. READCOMMAND reported success but left a non-numeric value behind,
         * which the DLL contract does not allow: errors are signalled by the return code only,
         * and the float out-parameter is meaningful whenever that code is 0. A NaN or infinity
         * therefore means the reading is untrustworthy - it must never reach a caller that would
         * use it as a force measurement.
         */
        const val NON_NUMERIC_VALUE = -800
    }

    override val message: String = when (errorCode) {
        -1 -> "Invalid argument value in function call."
        -2 -> "Cannot open or close serial port."
        -100 -> "No response."
        // Vendor wording: the instrument did answer, but the reply could not be decoded. A bad
        // station number is only one way for that to happen, so do not narrow it to that.
        -200 -> "Invalid response: there was a response, but the data could not be decoded."
        -300 -> "Invalid checksum."
        -400 -> "Not acknowledged (NAK)."
        -500 -> "Invalid reply length."
        -600 -> "Stream buffer full."
        -700 -> "No stream data."
        NON_NUMERIC_VALUE -> "Non-numeric value returned while the driver reported success."
        else -> "Unknown: $errorCode"
    }
}