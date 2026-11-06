plugins {
    // AGP 9 applies Kotlin itself — do NOT also apply org.jetbrains.kotlin.android
    // (duplicate "kotlin" extension). Versions live in gradle/libs.versions.toml.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
