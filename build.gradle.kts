plugins {
    kotlin("jvm") version "2.1.20"
    id("io.ktor.plugin") version "3.0.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.0.0")
    implementation("io.ktor:ktor-server-netty:3.0.0")
    implementation("io.ktor:ktor-server-sse:3.0.0")
    implementation("io.ktor:ktor-server-call-logging:3.0.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.ktor:ktor-server-websockets:3.0.0")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")
}

application {
    mainClass.set("ApplicationKt")
}