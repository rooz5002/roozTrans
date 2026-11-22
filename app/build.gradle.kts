import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ver = Properties().apply { rootProject.file("version.properties").inputStream().use { load(it) } }
val updateToken = rootProject.file("update.token").takeIf { it.exists() }?.readText()?.trim() ?: ""
// Private, untracked settings (see deploy.properties.example / keystore.properties.example)
fun props(name: String) = Properties().apply { rootProject.file(name).takeIf { it.exists() }?.inputStream()?.use { load(it) } }
val deploy = props("deploy.properties")
val upload = props("keystore.properties")

android {
    namespace = "com.rooz.translator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rooz.trans"
        minSdk = 30          // Android 11+: needed for AccessibilityService.takeScreenshot
        targetSdk = 36
        versionCode = ver.getProperty("versionCode").toInt()
        versionName = ver.getProperty("versionName")
        ndk { abiFilters += listOf("arm64-v8a") + (if (project.hasProperty("emu")) listOf("x86_64") else emptyList()) }
    }

    signingConfigs {
        if (upload.getProperty("storeFile") != null) create("upload") {
            storeFile = file(upload.getProperty("storeFile"))
            storePassword = upload.getProperty("storePassword")
            keyAlias = upload.getProperty("keyAlias")
            keyPassword = upload.getProperty("keyPassword")
        }
    }

    // direct = APK shared yourself, updates itself from your own server
    // play   = Google Play build (no self-updater; Play handles updates)
    flavorDimensions += "dist"
    productFlavors {
        create("direct") {
            dimension = "dist"
            buildConfigField("String", "UPDATE_URL", "\"${deploy.getProperty("updateUrl", "")}\"")
            buildConfigField("String", "UPDATE_TOKEN", "\"$updateToken\"")
            buildConfigField("boolean", "SELF_REPAIR", "true")
        }
        create("play") {
            dimension = "dist"
            buildConfigField("boolean", "SELF_REPAIR", "false")   // no WRITE_SECURE_SETTINGS on Play
            ndk { abiFilters += "armeabi-v7a" }   // Play splits per device, so older 32-bit phones cost nothing extra
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    // direct: keeps the key existing installs were signed with, so in-place updates keep working
    productFlavors.getByName("direct").signingConfig = signingConfigs.getByName("debug")
    // play: upload key (Google re-signs with the Play app-signing key)
    productFlavors.getByName("play").signingConfig = signingConfigs.findByName("upload")
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module", "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // On-device OCR (Chinese + Latin), model bundled inside the APK -> no download, works in China
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    // On-device translation (EN<->ZH packs downloaded once, before travel)
    implementation("com.google.mlkit:translate:17.0.3")

    // Claude (official SDK) + raw HTTP for OpenAI / DeepSeek / geo-IP
    implementation("com.anthropic:anthropic-java:3.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
