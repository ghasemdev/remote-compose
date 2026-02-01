import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
application {
    mainClass.set("com.parsomash.remote.compose.server.ApplicationKt")
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.call.logging)
    
    // Remote Compose - Server side (creation)
    implementation(libs.androidx.compose.remote.core)
    implementation(libs.androidx.compose.remote.creation.core)
    
    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Security
    implementation(libs.kotlinx.coroutines.core)
    
    // Logging
    implementation(libs.logback.classic)
    
    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
}

tasks.test {
    useJUnitPlatform()
}
