plugins {
    kotlin("jvm") version "1.9.23"
    `java`
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "ch.rupfizupfi.dscusb"
version = "0.0.1-beta.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.jnr:jnr-ffi:2.2.0")
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
    jvmToolchain(21)
}