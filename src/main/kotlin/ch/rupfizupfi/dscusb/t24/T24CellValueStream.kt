package ch.rupfizupfi.dscusb.t24

import ch.rupfizupfi.dscusb.Measurement
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Wireless counterpart to [ch.rupfizupfi.dscusb.dscusb.CellValueStream], with the same contract:
 * start it, poll [getNextValues], and treat [isReading] as the loop condition.
 *
 * The mechanism underneath is different. The wired stream pulls - it calls the driver in a loop. T24
 * transmitters push instead, so there is nothing to poll: the driver invokes [DataCallback] as packets
 * arrive and the reader thread only owns the lifecycle (pair, wake, then wait for a stop request).
 * Readings therefore appear at the transmitter's own pace, with gaps whenever the radio drops a
 * packet - do not expect an evenly spaced series. Packets are filtered to the paired transmitter's
 * data tag, so other transmitters on the same base station are ignored.
 *
 * Pairing parameters are constructor arguments because they depend on the installation; the defaults
 * target the first base station with a five-second pairing window.
 */
class T24CellValueStream(
    private val baseStation: Byte = 0,
    private val useRemoteSettings: Byte = 0,
    private val configMode: Byte = 0,
    private val pairingDuration: Byte = 5,
) {
    private val loadCellValues = ConcurrentLinkedQueue<Pair<Float, Long>>()
    private val connection = T24Connection()
    private var running = AtomicBoolean(false)
    private var inProgress = AtomicBoolean(false)
    private val lastError = AtomicReference<Throwable?>(null)
    private val pairedDevice = AtomicReference<Pair<Int, Int>?>(null)

    /**
     * Held for as long as the callback is registered. The driver keeps a raw function pointer to this
     * object, so letting it become unreachable would crash the JVM on the next packet rather than
     * throwing - a local would be eligible for collection the moment the reader thread parks.
     */
    private val dataCallback = object : DataCallback {
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
            // Runs on a driver thread, so this does the least work that still records the reading:
            // queue it and get out. Timestamped here because the packet carries no capture time.
            //
            // The callback is per base station, not per transmitter, so filter down to the tag this
            // stream paired with - otherwise a second transmitter's readings interleave with ours.
            // Fail open if pairing has not recorded a tag yet: dropping everything would be the worse
            // failure, since an always-empty queue is indistinguishable from a silent cell.
            val paired = pairedDevice.get()
            if (paired == null || paired.second == dataTag) {
                loadCellValues.add(Pair(value, System.currentTimeMillis()))
            }
            return 0
        }
    }

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
            // The reader owns the connection for its whole lifetime, so every exit path is handled
            // here - there is no caller left to propagate to. A throw from open() or pair() used to
            // kill this thread outright, leaving the port open, inProgress stuck true and the stream
            // claiming to run while delivering nothing.
            try {
                connection.open()
                pairedDevice.set(connection.pair(baseStation, useRemoteSettings, configMode, pairingDuration))
                connection.registerDataCallback(dataCallback)
                connection.wakeAll()

                while (running.get()) {
                    // Nothing to do but wait; packets arrive on the driver's thread. Short enough
                    // that stopReading() is not left hanging on a full interval.
                    Thread.sleep(100)
                }
            } catch (t: Throwable) {
                lastError.set(t)
            } finally {
                // running is cleared first so isReading() never claims a dead reader is alive.
                running.set(false)
                // Let the transmitters sleep, then clear the callback before the port goes away so
                // the driver cannot invoke into a callback whose connection is gone. Each step is
                // guarded: a failure to sleep must not skip the close.
                runCatching { connection.sleepAll() }
                runCatching { connection.unregisterCallbacks() }
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
            // The reader already exited, so its finally block will never run again to release the
            // flag - do it here, otherwise this instance refuses every later call.
            inProgress.set(false)
        }
    }

    /** False once the reader has stopped, whether it was asked to stop or died on an error. */
    fun isReading(): Boolean {
        return running.get()
    }

    /**
     * What ended the reader, or null if it was stopped on request or is still running.
     * A stream that ended by itself cannot be restarted - construct a new instance.
     */
    fun getLastError(): Throwable? {
        return lastError.get()
    }

    /** `(id, dataTag)` of the paired transmitter, or null until pairing succeeds. */
    fun getPairedDevice(): Pair<Int, Int>? {
        return pairedDevice.get()
    }

    fun getNextValues(): List<Measurement> {
        val values = mutableListOf<Measurement>()
        while (loadCellValues.isNotEmpty()) {
            values.add(Measurement.fromPair(loadCellValues.poll()))
        }
        return values
    }
}
