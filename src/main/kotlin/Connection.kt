package ch.rupfizupfi.dscusb

class Connection {
    private val dscusb: DSCUSB = DSCUSB()
    private var serialNumber: Int = 0

    fun open() {
        val port = dscusb.openPort()
        if (port == 1) {
            val deviceCount = dscusb.deviceCount()
            for (i in 1..deviceCount) {
                val serial = dscusb.serialNumber(i.toByte())
                if (serial != 0) {
                    serialNumber = serial
                    break
                }
            }
            if (serialNumber == 0) {
                throw CommandExecutionException(-2)
            }
        } else {
            throw CommandExecutionException(-2)
        }
    }

    fun close() {
        dscusb.closePort()
    }

    fun getDeviceCount(): Int {
        return dscusb.deviceCount()
    }

    fun getSerialNumber(): Int {
        return serialNumber
    }

    fun readLoadCellValue(): Float {
        return dscusb.readCommand(serialNumber, "SYS")
    }

    fun readReadRateValue(): Byte {
        val result =  dscusb.readCommand(serialNumber, "RATE")
        return result.toInt().toByte()
    }

    fun readBaudRateValue(): Byte {
        val result = dscusb.readCommand(serialNumber, "BAUD")
        return result.toInt().toByte()
    }

    fun getTimeout(): Long {
        return dscusb.getTimeout()
    }
}