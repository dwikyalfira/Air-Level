import java.util.Properties

plugins {
    // AGP 9 registers the Kotlin extension — do not apply kotlin.android here.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val iqAirApiKey: String = localProps.getProperty("IQAIR_API_KEY", "")

android {
    namespace = "com.airwear.aqi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.airwear.aqi"
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        buildConfigField("String", "IQAIR_API_KEY", "\"$iqAirApiKey\"")
        buildConfigField("String", "IQAIR_BASE_URL", "\"https://api.airvisual.com/\"")
    }

    buildTypes {
        release {
            // Personal sideload build: sign with the local debug key so
            // `installRelease` works over ADB. Swap for a real key before Play.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.compose.material.icons.core)
    implementation(libs.activity.compose)
    // play-services pulls Fragment 1.0, which breaks ActivityResult permission callbacks
    implementation(libs.fragment)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Complications
    implementation(libs.watchface.complications.data.source.ktx)

    // Tiles
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.kotlinx.coroutines.guava)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Location
    implementation(libs.play.services.location)

    // Background
    implementation(libs.work.runtime.ktx)

    // DataStore
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
