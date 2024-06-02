package ch.rupfizupfi.dscusb

class CommandExecutionException(errorCode: Int) : Exception() {
    override val message: String = when (errorCode) {
        -1 -> "Invalid argument value in function call."
        -2 -> "Cannot open or close serial port."
        -100 -> "No response."
        -200 -> "Invalid station number in response."
        -300 -> "Invalid checksum."
        -400 -> "Not acknowledged (NAK)."
        -500 -> "Invalid reply length."
        -600 -> "Stream buffer full."
        -700 -> "No stream data."
        else -> "Unknown: $errorCode"
    }
}