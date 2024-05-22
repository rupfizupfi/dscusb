plugins {
    kotlin("jvm") version "1.9.23"
    `java`
}

group = "ch.rupfizupfi.dscusb"
version = "0.0.1-beta.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.jnr:jnr-ffi:2.2.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}