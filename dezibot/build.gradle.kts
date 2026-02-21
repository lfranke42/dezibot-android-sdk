import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dokka)
    id("maven-publish")
}

android {
    namespace = "de.htwk.dezibot"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 26

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                version = "1.0.0"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    implementation(libs.coroutines)
    implementation(libs.java.websocket)
    ksp(libs.moshi.codegen)
}

dokka {
    dokkaSourceSets.main {
        documentedVisibilities(
            org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public
        )

        perPackageOption {
            matchingRegex.set("de\\.htwk\\.dezibot\\.internal.*")
            suppress.set(true)
        }
    }

    // Suppress build-variant source sets — they only contain KSP-generated Moshi adapters
    dokkaSourceSets.configureEach {
        if (name == "debug" || name == "release") {
            suppress.set(true)
        }
    }
}
