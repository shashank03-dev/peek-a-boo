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
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.shashank.peekaboo"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "2.0.3"

        // Sideloaded builds have no Play billing to buy Pro through, so they can ship with Pro
        // unlocked: ./gradlew :app:assembleRelease -PunlockPro=true. Play builds leave it off.
        buildConfigField("boolean", "PRO_UNLOCKED", (findProperty("unlockPro")?.toString() == "true").toString())

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
    testOptions {
        unitTests {
            // Robolectric needs the merged resources to render the real UI on the JVM.
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                it.maxHeapSize = "3g"
                // Robolectric reaches into JDK internals that Java 17+ hides by default.
                it.jvmArgs(
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
            }
        }
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
    implementation(libs.play.billing)

    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
