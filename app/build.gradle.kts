import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "dev.shashank.peekaboo"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.shashank.peekaboo"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "2.4.0"

        ndk {
            // Phones are ARM; dropping emulator ABIs halves the APK.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        // Sideload key committed with the repo so every build (local or CI) can update an
        // installed copy in place. Override with PEEKABOO_KEYSTORE* env vars for a private key.
        create("release") {
            val storePath = System.getenv("PEEKABOO_KEYSTORE") ?: keystoreProps.getProperty("storeFile") ?: "app/peekaboo-sideload.jks"
            storeFile = rootProject.file(storePath)
            storePassword = System.getenv("PEEKABOO_KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword") ?: "peekaboo"
            keyAlias = System.getenv("PEEKABOO_KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias") ?: "peekaboo"
            keyPassword = System.getenv("PEEKABOO_KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword") ?: "peekaboo"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.face)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.haze)
    implementation(libs.coil.compose)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play)

    testImplementation("junit:junit:4.13.2")
}
