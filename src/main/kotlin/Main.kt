package ch.rupfizupfi.dscusb

import ch.rupfizupfi.dscusb.T24.DataCallback
import ch.rupfizupfi.dscusb.T24.WakeUpCallback

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
t24wifi()
}

fun t24wifi() {
    val connection = T24Connection();
    connection.open();
    connection.pair(0, 0, 0, 0)
    println("Read load cell value: ${connection.readLoadCellValue()}")

    connection.registerDataCallback(object : DataCallback {
        override fun call(baseStation: Byte, dataTag: Long, value: Float, status: Byte, error: Byte, lowBatt: Byte, rssi: Byte, cv: Byte): Int {
            println("Callback: $baseStation, $dataTag, $value, $status, $error, $lowBatt, $rssi, $cv")
            return 0
        }
    })

    connection.registerWakeUpCallback(object : WakeUpCallback {
        override fun call(baseStation: Byte, id: Long, rssi: Byte, cv: Byte): Int {
            println("Callback: $baseStation, $id, $rssi, $cv")
            return 0
        }
    })

    connection.wakeAll()

    Thread.sleep(10000)

    println("Close port: ${connection.close()}")
}

fun dscusb() {
    val dscusb = DSCUSB()
    println("Version: ${dscusb.version()}")
    println("Open port: ${dscusb.openPort()}")
    println("Device count: ${dscusb.deviceCount()}")
    println("Serial number: ${dscusb.serialNumber(0)}")
    println("Report: ${dscusb.report()}")

    if(dscusb.deviceCount()>0){
        dscusb.closePort()

        val cvs = CellValueStream();
        cvs.startReading();
        while (true){
            val values = cvs.getNextValues()
            //print values to out
            println(values)
            Thread.sleep(100)
        }
    }
    else {
        dscusb.closePort()
    }
}