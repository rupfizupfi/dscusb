package ch.rupfizupfi.dscusb.deck

import ch.rupfizupfi.deck.device.api.ContractVersion
import ch.rupfizupfi.deck.device.api.DeviceApi
import ch.rupfizupfi.deck.device.api.LoadCellStream
import ch.rupfizupfi.deck.device.api.LoadCellStreamProvider
import ch.rupfizupfi.dscusb.dscusb.CellValueStream
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional

/**
 * What makes this jar a deck plugin: registered via
 * `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, because this
 * package sits outside the deck's `ch.rupfizupfi.deck` component-scan root — on the classpath the
 * provider bean appears, off it nothing does.
 *
 * The mode condition mirrors the deck's simulated providers exactly, so the two can never both
 * register and `simulated` can never resolve to hardware. Spring stays compileOnly and out of the
 * shadow jar; the deck supplies it at runtime.
 *
 * [OnWindowsCondition] keeps a platform this driver cannot work on from looking like a working one.
 */
@AutoConfiguration
@ConditionalOnProperty(name = ["deck.hardware.mode"], havingValue = "real", matchIfMissing = true)
@Conditional(OnWindowsCondition::class)
class DeckLoadCellAutoConfiguration {

    init {
        // In the initializer, not a @Bean method: this jar is file-dropped onto the deck's
        // loader.path, so a skewed contract must stop startup before any provider bean exists.
        //
        // The argument must stay the literal ContractVersion.VALUE. It is a compile-time constant
        // and is inlined into THIS class file, which is what makes it the version this jar was
        // built against. Do not "simplify" it away or read the version at runtime - either turns
        // the check into a comparison of the deployed contract with itself, which always passes.
        DeviceApi.verifyPluginBuiltAgainst(ContractVersion.VALUE)
    }

    /** A new stream per call — a stopped [CellValueStream] can never be restarted. */
    @Bean
    fun cellValueStreamProvider(): LoadCellStreamProvider = object : LoadCellStreamProvider {
        override fun open(): LoadCellStream = CellValueStreamAdapter(CellValueStream())
    }
}
