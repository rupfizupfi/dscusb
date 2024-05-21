package com.glowingblue.intellij
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val dscusb = DSCUSB()
    println("Version: ${dscusb.version()}")
    println("Device count: ${dscusb.deviceCount()}")
    println("Serial number: ${dscusb.serialNumber(0)}")
    println("Report: ${dscusb.report()}")
}