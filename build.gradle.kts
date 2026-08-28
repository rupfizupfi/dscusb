import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    `java`
    application
    id("com.gradleup.shadow") version "9.6.1"
    `maven-publish`
}

group = "ch.rupfizupfi.dscusb"
version = providers.gradleProperty("version").get()

repositories {
    mavenCentral()
    // Reached only when there is no sibling deck checkout to composite-include (see
    // settings.gradle.kts) — with one, the substitution answers first and this is never queried.
    // Content-filtered because GitHub Packages demands a token even for public reads: unfiltered,
    // it would be asked for every dependency and each would then need credentials. Empty
    // credentials are deliberate for the same reason — a sibling build must not need a token.
    maven {
        name = "GitHubPackagesDeviceApi"
        url = uri("https://maven.pkg.github.com/rupfizupfi/breaktest-command-deck")
        credentials {
            username = providers.environmentVariable("GITHUB_ACTOR").getOrElse("")
            password = providers.environmentVariable("GITHUB_TOKEN").getOrElse("")
        }
        content { includeGroup("ch.rupfizupfi.deck") }
    }
}

dependencies {
    implementation("com.github.jnr:jnr-ffi:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")

    // compileOnly on purpose: the deck provides both at runtime, and shadowJar must not bundle
    // them - a copy of the contract classes inside this jar would shadow the deck's own.
    compileOnly("ch.rupfizupfi.deck:device-api:1.0.0")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:4.1.0")
    // Same reason: the deck brings slf4j-api, and only the logging API is used.
    compileOnly("org.slf4j:slf4j-api:2.0.17")
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

// The shadow jar IS the published artifact: the deck resolves it by coordinates and drops it into
// its runtime plugin directory. Publishing the task rather than components["shadow"] because the
// shadow component keeps shadowJar's `all` classifier, and the deck asks for the plain coordinate.
// The pom carries no dependencies, which is correct - jnr-ffi and kotlin-stdlib are inside the jar,
// and device-api / spring-boot-autoconfigure are compileOnly because the deck provides them.
publishing {
    publications {
        create<MavenPublication>("shadow") {
            groupId = "ch.rupfizupfi.dscusb"
            artifactId = "dscusb"
            artifact(tasks.shadowJar) { classifier = "" }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/rupfizupfi/dscusb")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

kotlin {
    jvmToolchain(26)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_26
    }
}
