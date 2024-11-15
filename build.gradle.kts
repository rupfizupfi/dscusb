plugins {
    kotlin("jvm") version "1.9.23"
    `java`
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "ch.rupfizupfi.dscusb"
version = project.properties["version"].toString()

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.jnr:jnr-ffi:2.2.17")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
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
    jvmToolchain(23)
}