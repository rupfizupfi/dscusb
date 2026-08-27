package ch.rupfizupfi.dscusb.deck

import ch.rupfizupfi.deck.device.api.LoadCellStream
import ch.rupfizupfi.deck.device.api.LoadCellStreamProvider
import ch.rupfizupfi.dscusb.dscusb.CellValueStream
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

/**
 * What makes this jar a deck plugin: registered via
 * `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, because this
 * package sits outside the deck's `ch.rupfizupfi.deck` component-scan root — on the classpath the
 * provider bean appears, off it nothing does.
 *
 * The mode condition mirrors the deck's simulated providers exactly, so the two can never both
 * register and `simulated` can never resolve to hardware. Spring stays compileOnly and out of the
 * shadow jar; the deck supplies it at runtime.
 */
@AutoConfiguration
@ConditionalOnProperty(name = ["deck.hardware.mode"], havingValue = "real", matchIfMissing = true)
class DeckLoadCellAutoConfiguration {

    /** A new stream per call — a stopped [CellValueStream] can never be restarted. */
    @Bean
    fun cellValueStreamProvider(): LoadCellStreamProvider = object : LoadCellStreamProvider {
        override fun open(): LoadCellStream = CellValueStreamAdapter(CellValueStream())
    }
}
