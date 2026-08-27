package ch.rupfizupfi.dscusb.deck

import ch.rupfizupfi.deck.device.api.LoadCellStream
import ch.rupfizupfi.deck.device.api.StreamFailure
import ch.rupfizupfi.dscusb.CommandExecutionException
import ch.rupfizupfi.dscusb.dscusb.CellValueStream
import ch.rupfizupfi.deck.device.api.Measurement as DeckMeasurement

/**
 * The deck's [LoadCellStream] over [CellValueStream]. Pure delegation by rule: this package is code
 * the deck's simulated path never exercises, so anything clever here first runs on the bench. The
 * two mappings below carry no policy — they translate types only.
 *
 * The five methods line up one-for-one with [LoadCellStream] because this driver is shaped to that
 * contract deliberately; the contract itself is compile-checked through the composite-included
 * `device-api` build (see settings.gradle.kts).
 */
class CellValueStreamAdapter(private val stream: CellValueStream) : LoadCellStream {

    override fun startReading() = stream.startReading()

    override fun stopReading() = stream.stopReading()

    override fun getNextValues(): List<DeckMeasurement> =
        stream.getNextValues().map { DeckMeasurement(it.getForce(), it.getTimestamp()) }

    override fun isReading(): Boolean = stream.isReading()

    override fun lastError(): StreamFailure? {
        val error = stream.getLastError() ?: return null

        // The numeric code is kept verbatim: it maps onto the READCOMMAND table in the vendor docs.
        // Anything else carries no code at all, and null is what lets the deck tell the two apart
        // without this adapter having to word anything.
        val driverCode = (error as? CommandExecutionException)?.errorCode?.toString()

        return StreamFailure(driverCode, error.javaClass.simpleName, error.message)
    }
}
