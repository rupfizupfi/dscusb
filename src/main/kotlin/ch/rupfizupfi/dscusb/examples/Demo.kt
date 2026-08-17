package ch.rupfizupfi.dscusb.examples

import ch.rupfizupfi.dscusb.dscusb.CellValueStream
import ch.rupfizupfi.dscusb.dscusb.DSCUSB
import ch.rupfizupfi.dscusb.t24.DataCallback
import ch.rupfizupfi.dscusb.t24.T24Connection
import ch.rupfizupfi.dscusb.t24.WakeUpCallback

/**
 * Hardware smoke test for both backends. Needs a real device and the matching native DLL on
 * the library path - see doc/native-libraries.md.
 *
 *     ./gradlew run --args="dscusb"   (wired DSC USB cell, the default)
 *     ./gradlew run --args="t24"      (wireless T24 base station)
 */
fun main(args: Array<String>) {
    when (val target = args.firstOrNull() ?: "dscusb") {
        "dscusb" -> dscusbDemo()
        "t24" -> t24Demo()
        else -> println("Unknown target '$target'. Expected 'dscusb' or 't24'.")
    }
}

/** Reads the wired cell for 10s, printing whatever the background reader has queued up. */
private fun dscusbDemo() {
    val dscusb = DSCUSB()
    println("DLL version:   ${dscusb.version()}")
    println("Open port:     ${dscusb.openPort()}")
    println("Device count:  ${dscusb.deviceCount()}")
    println("Serial number: ${dscusb.serialNumber(0)}")
    println("Report:        ${dscusb.report()}")

    val deviceCount = dscusb.deviceCount()
    // The stream opens its own connection, so hand the port back before starting it.
    dscusb.closePort()

    if (deviceCount == 0) {
        println("No device found - nothing to stream.")
        return
    }

    val stream = CellValueStream()
    stream.startReading()

    val deadline = System.currentTimeMillis() + 10_000
    while (System.currentTimeMillis() < deadline) {
        // A reader that died on a driver error clears isReading() and parks the cause here.
        if (!stream.isReading()) {
            println("Reader stopped: ${stream.getLastError() ?: "on request"}")
            return
        }
        stream.getNextValues().forEach(::println)
        Thread.sleep(100)
    }

    stream.stopReading()
    println("Done.")
}

/** Pairs with a wireless transmitter and prints data/wake-up packets for 10s. */
private fun t24Demo() {
    val connection = T24Connection()
    connection.open()

    val (id, dataTag) = connection.pair(0, 0, 0, 0)
    println("Paired: id=$id dataTag=$dataTag")
    println("Read load cell value: ${connection.readLoadCellValue()}")

    connection.registerDataCallback(object : DataCallback {
        override fun call(
            baseStation: Byte,
            dataTag: Int,
            value: Float,
            status: Byte,
            error: Byte,
            lowBatt: Byte,
            rssi: Byte,
            cv: Byte
        ): Int {
            println("Data: base=$baseStation tag=$dataTag value=$value status=$status error=$error lowBatt=$lowBatt rssi=$rssi cv=$cv")
            return 0
        }
    })

    connection.registerWakeUpCallback(object : WakeUpCallback {
        override fun call(baseStation: Byte, id: Int, rssi: Byte, cv: Byte): Int {
            println("Wake-up: base=$baseStation id=$id rssi=$rssi cv=$cv")
            return 0
        }
    })

    connection.wakeAll()
    Thread.sleep(10_000)

    connection.sleepAll()
    connection.close()
    println("Port closed.")
}
