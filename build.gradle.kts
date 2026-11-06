plugins {
    // AGP 9 pairs with Gradle 9 (Studio JBR 25). AGP 9 applies Kotlin itself —
    // do NOT also apply org.jetbrains.kotlin.android (duplicate "kotlin" extension).
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}
