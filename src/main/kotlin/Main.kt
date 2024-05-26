package com.glowingblue.intellij
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
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