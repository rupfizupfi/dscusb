import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    `java`
    application
    id("com.gradleup.shadow") version "9.6.1"
}

group = "ch.rupfizupfi.dscusb"
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.jnr:jnr-ffi:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")
}

// Only the bundled hardware smoke test in ch.rupfizupfi.dscusb.examples - consumers use this
// project as a library. See doc/native-libraries.md for making the DLLs visible to `run`.
application {
    mainClass = "ch.rupfizupfi.dscusb.examples.DemoKt"
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    shadowJar {
        archiveFileName.set("dscusb.jar")
    }
}

kotlin {
    jvmToolchain(26)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_26
    }
}
