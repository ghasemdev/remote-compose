import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest)
}

android {
    namespace = "com.parsomash.remote.compose"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.parsomash.remote.compose"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/INDEX.LIST",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.set(
            listOf(
                "-Xconsistent-data-class-copy-visibility",
                "-Xannotation-default-target=param-property",
            ),
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Remote Compose - Server side (creation)
    implementation(libs.androidx.compose.remote.core)
    implementation(libs.androidx.compose.remote.creation)
    implementation(libs.androidx.compose.remote.creation.core)
    implementation(libs.androidx.compose.remote.creation.compose)
    implementation(libs.androidx.compose.remote.tooling.preview)

    // Remote Compose - Client side (player)
    implementation(libs.androidx.compose.remote.core)
    implementation(libs.androidx.compose.remote.player.core)
    implementation(libs.androidx.compose.remote.player.view)
    implementation(libs.androidx.compose.remote.player.compose)
    implementation(libs.androidx.compose.remote.tooling.preview)

    // Ktor Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Security
    implementation(libs.bouncycastle.provider)
    implementation(libs.bouncycastle.pkix)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.test.mockk.core)

    // Android Testing
    androidTestImplementation(libs.bundles.android.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.test.mockk.android)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.slf4j)
    debugRuntimeOnly(libs.logback.classic)
}
