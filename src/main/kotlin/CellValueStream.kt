package com.glowingblue.intellij

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class CellValueStream {
    private val loadCellValues = ConcurrentLinkedQueue<Float>()
    private val connection = Connection()
    private var running = AtomicBoolean(false)
    private var inProgress = AtomicBoolean(false)

    fun startReading() {
        if (inProgress.getAndSet(true)) {
            throw IllegalStateException("Cannot start reading while another operation is in progress.")
        }

        if(running.get()) {
            throw IllegalStateException("Cannot start reading while already running.")
        }

        connection.open()
        running.set(true)
        thread(start = true) {
            while (running.get()) {
                val loadCellValue = readLoadCellValue()
                loadCellValues.add(loadCellValue)
                Thread.sleep(connection.getTimeout())
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

    fun getNextValues(): List<Float> {
        val values = mutableListOf<Float>()
        while (loadCellValues.isNotEmpty()) {
            values.add(loadCellValues.poll())
        }
        return values
    }

    private fun readLoadCellValue(): Float {
        return connection.readLoadCellValue()
    }
}