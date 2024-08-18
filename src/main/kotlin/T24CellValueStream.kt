package ch.rupfizupfi.dscusb

import ch.rupfizupfi.dscusb.T24.DataCallback
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class T24CellValueStream {
    private val loadCellValues = ConcurrentLinkedQueue<Pair<Float, Long>>()
    private val connection = T24Connection()
    private var running = AtomicBoolean(false)
    private var inProgress = AtomicBoolean(false)

    fun startReading() {
        if (inProgress.getAndSet(true)) {
            throw IllegalStateException("Cannot start reading while another operation is in progress.")
        }

        if (running.get()) {
            throw IllegalStateException("Cannot start reading while already running.")
        }

        running.set(true)

        thread(start = true) {
            connection.open()
            connection.pair(1, 1, 1, 5)
            connection.registerDataCallback(
                object: DataCallback {
                    override fun call(baseStation: Byte, dataTag: Long, value: Float, status: Byte, error: Byte, lowBatt: Byte, rssi: Byte, cv: Byte): Int {
                        println("Callback: $baseStation, $dataTag, $value, $status, $error, $lowBatt, $rssi, $cv")
                        return 0
                    }
                }
            )

            connection.wakeAll()

            while (running.get()) {
                Thread.sleep(500)
            }

            connection.sleepAll()
            connection.close()
            inProgress.set(false)
        }

        inProgress.set(false)
    }

    fun stopReading() {
        if (inProgress.getAndSet(true)) {
            throw IllegalStateException("Cannot stop reading while another operation is in progress.")
        }
        running.set(false)
    }

    fun getNextValues(): List<Measurement> {
        val values = mutableListOf<Measurement>()
        while (loadCellValues.isNotEmpty()) {
            values.add(Measurement.fromPair(loadCellValues.poll()))
        }
        return values
    }
}