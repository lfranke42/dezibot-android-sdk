plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    id("org.jetbrains.dokka") version "2.1.0"
}

dependencies {
    dokkaPlugin(libs.android.documentation.plugin)
}
