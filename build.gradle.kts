// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Repositories and plugins are defined in settings.gradle.kts
// Do NOT add repositories or buildscript here when using FAIL_ON_PROJECT_REPOS

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidx.room) apply false
}