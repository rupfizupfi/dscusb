package ch.rupfizupfi.dscusb.deck

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

/**
 * Gates the provider on the only platform it can work on.
 *
 * `DSCUSBDrv64.dll` is a Win32 library and Mantracourt ships no Linux build, so on any other OS
 * [ch.rupfizupfi.dscusb.dscusb.DSCUSB]'s constructor throws `UnsatisfiedLinkError` the moment the
 * deck opens a stream. Without this gate the bean registers happily, the deck's `HardwareModeCheck`
 * sees a provider and lets startup through, and the failure lands mid-run when a test starts.
 *
 * Refusing to register instead turns it into a startup refusal — the deck already fails loudly when
 * a provider is missing. The warning below is what tells the operator *why* the jar it can see did
 * not register.
 */
internal class OnWindowsCondition : Condition {

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val os = System.getProperty("os.name").orEmpty()
        if (os.startsWith("Windows")) {
            return true
        }

        LoggerFactory.getLogger(OnWindowsCondition::class.java).warn(
            "dscusb driver plugin: NOT registering LoadCellStreamProvider. The load cell is reached " +
                "through DSCUSBDrv64.dll, a Windows-only vendor library with no Linux build, and " +
                "this JVM runs on '{}'. The deck will now refuse to start in real mode and name " +
                "this jar as missing - the jar is present, the platform is wrong. A containerised " +
                "deck cannot drive this hardware.",
            os,
        )
        return false
    }
}
