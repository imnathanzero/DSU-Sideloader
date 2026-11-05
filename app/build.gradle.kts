import java.util.Base64
import java.util.Properties

fun getReleaseSigningConfig(): File {
    return File(".sign/dsu_sideloader.prop")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("com.mikepenz.aboutlibraries.plugin")
    id("kotlinx-serialization")
    id("org.jmailen.kotlinter")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    val versionCode = rootProject.extra["versionCode"] as Int
    val versionName = rootProject.extra["versionName"] as String
    val packageName = rootProject.extra["packageName"] as String

    namespace = packageName
    compileSdk = 35

    defaultConfig {
        this.applicationId = packageName
        this.versionCode = versionCode
        this.versionName = versionName

        minSdk = 29
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        val keystoreBase64 = System.getenv("SIGNING_KEYSTORE_BASE64")
        val releaseSigningConfig = getReleaseSigningConfig()
        if (!keystoreBase64.isNullOrBlank()) {
            // CI: keystore + credentials supplied via GitHub Actions secrets
            // (SIGNING_KEYSTORE_BASE64 / SIGNING_STORE_PASSWORD / SIGNING_KEY_ALIAS / SIGNING_KEY_PASSWORD).
            create("release") {
                val keystoreFile = File.createTempFile("release-signing", ".jks")
                keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64.trim()))
                storeFile = keystoreFile
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        } else if (releaseSigningConfig.exists()) {
            // Local: .sign/dsu_sideloader.prop
            // keystore=some/path/keystore.jks / keystore_pw=... / alias=... / alias_pw=...
            create("release") {
                val props = Properties()
                props.load(releaseSigningConfig.inputStream())

                storeFile = File(props.getProperty("keystore"))
                storePassword = props.getProperty("keystore_pw")
                keyAlias = props.getProperty("alias")
                keyPassword = props.getProperty("alias_pw")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("miniDebug") {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

aboutLibraries {
    // Remove the "generated" timestamp to allow for reproducible builds
    excludeFields = arrayOf("generated")
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}

dependencies {
    implementation(AndroidX.appCompat)
    implementation(AndroidX.dataStore.preferences)

    implementation(AndroidX.activity.compose)
    implementation(AndroidX.lifecycle.viewModelCompose)
    implementation(AndroidX.navigation.compose)
    implementation(AndroidX.compose.material3)
    implementation(AndroidX.compose.material)
    implementation(AndroidX.compose.runtime.liveData)
    implementation(AndroidX.compose.material.icons.extended)
    implementation(AndroidX.compose.ui.toolingPreview)
    implementation(AndroidX.compose.ui)

    implementation(AndroidX.core.ktx)
    implementation(AndroidX.fragment.ktx)
    implementation(AndroidX.preference.ktx)
    implementation(AndroidX.lifecycle.runtime.ktx)

    implementation(Google.dagger.hilt.android)
    implementation(AndroidX.hilt.navigationCompose)
    ksp(Google.dagger.hilt.compiler)

    implementation(Google.android.material)
    implementation(KotlinX.serialization.json)

    implementation("com.github.topjohnwu.libsu:core:_")
    implementation("com.github.topjohnwu.libsu:service:_")

    implementation("org.tukaani:xz:_")
    implementation("org.apache.commons:commons-compress:_")

    implementation("com.mikepenz:aboutlibraries-core:_")

    implementation("dev.rikka.shizuku:api:_")
    implementation("dev.rikka.shizuku:provider:_")

    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:_")

    compileOnly(project(":hidden-api-stub"))

    testImplementation("junit:junit:4.13.2")
}

tasks {
    "preBuild" {
        dependsOn(lintKotlin)
    }
    "lintKotlin" {
        dependsOn(formatKotlin)
    }
}
