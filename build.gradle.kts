plugins {
    kotlin("jvm") version "2.1.10"
    `java`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ch.rupfizupfi.dscusb"
version = project.properties["version"].toString()

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.jnr:jnr-ffi:2.2.17")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
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
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "23"
        }
    }
}