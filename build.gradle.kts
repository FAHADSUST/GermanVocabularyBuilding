// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 has built-in Kotlin support, so the kotlin-android plugin is NOT applied.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}