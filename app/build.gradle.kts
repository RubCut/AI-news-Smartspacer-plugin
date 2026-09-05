import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * Every build is signed with the same key so updates install over each other
 * instead of failing with a signature mismatch.
 *
 * By default the key checked into `signing/ainews.p12` is used. A fork can
 * override it with a real one through `signing.properties` or the environment
 * (AI_NEWS_KEYSTORE / AI_NEWS_KEYSTORE_PASSWORD / AI_NEWS_KEY_ALIAS /
 * AI_NEWS_KEY_PASSWORD) without touching this file.
 */
val signingProperties = Properties().apply {
    val file = rootProject.file("signing.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String, fallback: String): String =
    signingProperties.getProperty(key) ?: System.getenv(env) ?: fallback

val keystoreFile: File = rootProject.file(
    signingValue("storeFile", "AI_NEWS_KEYSTORE", "signing/ainews.p12")
)

android {
    namespace = "com.rubcut.ainews"
    // Material 1.14 depends on AndroidX lines that require API 36.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rubcut.ainews"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("shared") {
            storeFile = keystoreFile
            storeType = "PKCS12"
            storePassword = signingValue("storePassword", "AI_NEWS_KEYSTORE_PASSWORD", "ainews")
            keyAlias = signingValue("keyAlias", "AI_NEWS_KEY_ALIAS", "ainews")
            keyPassword = signingValue("keyPassword", "AI_NEWS_KEY_PASSWORD", "ainews")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        // Debug and release share the key, so an APK from Actions and a local
        // build can replace each other.
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("com.kieronquinn.smartspacer:sdk-plugin:1.1")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
}
