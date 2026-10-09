// Top-level build file for Gastoskwela

plugins {
    alias(libs.plugins.android.application) apply false

    // KSP for Room Database
    id("com.google.devtools.ksp") version "2.3.10" apply false
}
