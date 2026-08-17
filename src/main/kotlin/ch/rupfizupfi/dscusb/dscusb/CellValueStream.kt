package ch.rupfizupfi.dscusb.dscusb

import ch.rupfizupfi.dscusb.Measurement
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class CellValueStream {
    private val loadCellValues = ConcurrentLinkedQueue<Pair<Float, Long>>()
    private val connection = Connection()
    private var running = AtomicBoolean(false)
    private var inProgress = AtomicBoolean(false)
    private val lastError = AtomicReference<Throwable?>(null)

    fun startReading() {
        if (inProgress.getAndSet(true)) {
            throw IllegalStateException("Cannot start reading while another operation is in progress.")
        }

        if (running.get()) {
            throw IllegalStateException("Cannot start reading while already running.")
        }

        lastError.set(null)
        running.set(true)

        thread(start = true) {
            // The reader owns the connection for its whole lifetime, so every exit path has to be
            // handled here - there is no caller left to propagate to. Previously an unplugged cell
            // or any driver error killed this thread on the spot, skipping connection.close() and
            // leaking the native port, while the stream still reported itself running and simply
            // delivered nothing forever.
            try {
                connection.open()
                while (running.get()) {
                    loadCellValues.add(readLoadCellValue())
                }
            } catch (t: Throwable) {
                lastError.set(t)
            } finally {
                // running is cleared first so isReading() never claims a dead reader is alive.
                running.set(false)
                runCatching { connection.close() }
                inProgress.set(false)
            }
        }

        inProgress.set(false)
    }

    fun stopReading() {
        if (inProgress.getAndSet(true)) {
            throw IllegalStateException("Cannot stop reading while another operation is in progress.")
        }
        if (!running.getAndSet(false)) {
            // The reader already exited - on request earlier, or on an error - so its finally block
            // will never run again to release the flag. Do it here, otherwise inProgress stays set
            // and this instance throws on every later call, including a second stopReading().
            inProgress.set(false)
        }
    }

    /** False once the reader has stopped, whether it was asked to stop or died on an error. */
    fun isReading(): Boolean {
        return running.get()
    }

    /**
     * What ended the read loop, or null if it was stopped on request or is still running.
     * A stream that ended by itself cannot be restarted - construct a new instance.
     */
    fun getLastError(): Throwable? {
        return lastError.get()
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