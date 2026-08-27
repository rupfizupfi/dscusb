plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "dscusb"

// The deck owns the hardware contract (ch.rupfizupfi.deck:device-api); compiling this repo against
// the live contract is what guarantees the driver still matches it. Needs the
// breaktest-command-deck checkout as a sibling directory.
includeBuild("../breaktest-command-deck/device-api")