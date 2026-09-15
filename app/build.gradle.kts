plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.one.cognitivecompanion"
    val configuredApiBaseUrl = providers.gradleProperty("oneApiBaseUrl").orNull
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.one.cognitivecompanion"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // The emulator reaches a backend running on the development machine
        // through 10.0.2.2. Release configuration will provide an HTTPS URL.
        buildConfigField(
            "String",
            "ONE_API_BASE_URL",
            "\"${configuredApiBaseUrl ?: "http://10.0.2.2:8000/api/v1"}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            buildConfigField("Boolean", "ONE_PRODUCTION_BUILD", "true")
            buildConfigField("String", "ONE_API_BASE_URL", "\"${configuredApiBaseUrl ?: "https://configure-me.invalid/api/v1"}\"")
        }
        debug {
            buildConfigField("Boolean", "ONE_PRODUCTION_BUILD", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.livekit.android)
    implementation(libs.livekit.android.compose.components)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.google.arcore)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
