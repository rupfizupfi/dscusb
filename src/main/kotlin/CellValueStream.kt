package ch.rupfizupfi.dscusb

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class CellValueStream {
    private val loadCellValues = ConcurrentLinkedQueue<Pair<Float, Long>>()
    private val connection = Connection()
    private var running = AtomicBoolean(false)
    private var inProgress = AtomicBoolean(false)

    fun startReading() {
        if (inProgress.getAndSet(true)) {
            throw IllegalStateException("Cannot start reading while another operation is in progress.")
        }

        if (running.get()) {
            throw IllegalStateException("Cannot start reading while already running.")
        }

        connection.open()
        running.set(true)
        thread(start = true) {
            while (running.get()) {
                loadCellValues.add(readLoadCellValue())
            }
        }
        inProgress.set(false)
    }

    fun stopReading() {
        if (inProgress.getAndSet(true)) {
            throw IllegalStateException("Cannot stop reading while another operation is in progress.")
        }
        running.set(false)
        connection.close()
        inProgress.set(false)
    }

    fun getNextValues(): List<Measurement> {
        val values = mutableListOf<Measurement>()
        while (loadCellValues.isNotEmpty()) {
            values.add(Measurement.fromPair(loadCellValues.poll()))
        }
        return values
    }

    private fun readLoadCellValue(): Pair<Float,Long> {
        return Pair(connection.readLoadCellValue(), System.currentTimeMillis())
    }
}