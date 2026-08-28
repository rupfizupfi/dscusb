plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "dscusb"

// The deck owns the hardware contract (ch.rupfizupfi.deck:device-api); compiling this repo against
// the live contract is what guarantees the driver still matches it. The sibling checkout therefore
// stays PREFERRED — with it present, contract drift fails this build immediately.
//
// Without it (CI, a second machine, anyone who cloned only this repo) fall back to the published
// artifact, resolved from the deck's GitHub Packages repository declared in build.gradle.kts. That
// fallback pins a version instead of tracking the contract, so it cannot detect drift; what covers
// the gap is DeviceApi.verifyPluginBuiltAgainst, which refuses the jar at the deck's startup.
val deviceApi = file("../breaktest-command-deck/device-api")
if (deviceApi.isDirectory) {
    includeBuild(deviceApi)
} else {
    logger.lifecycle("dscusb: no sibling breaktest-command-deck checkout - device-api resolves "
            + "from GitHub Packages, so contract drift will surface at the deck's startup instead "
            + "of here.")
}
