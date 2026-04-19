plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
import java.util.Properties

val envProps = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.reelvoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.reelvoice"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-O3"
                arguments += "-DANDROID_STL=c++_shared"
                // 16 KB page size compatibility (Pixel 9+, newer arm64 devices).
                // Required by Play Store starting November 2025 for new targets.
                arguments += "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = envProps.getProperty("SIGNING_KEYSTORE", "keystore/bolsaaf-release.jks")
            storeFile = rootProject.file(keystorePath)
            storePassword = envProps.getProperty("SIGNING_STORE_PASSWORD")
            keyAlias = envProps.getProperty("SIGNING_ALIAS")
            keyPassword = envProps.getProperty("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // CameraX for in-app video recording (Live screen + Home "Record Live" video option)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    // CameraX exposes Guava's ListenableFuture in its public API; since AGP/SDK 35
    // no longer pulls it transitively, bring in real Guava (android flavor).
    implementation("com.google.guava:guava:33.0.0-android")

    // Google Mobile Ads (AdMob) — app registration only for now; no ad units placed yet.
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Google Play Billing — Pro subscription unlock.
    // Server validation of purchase tokens goes via /voice/billing/validate/ (see BILLING_CONTRACT.md).
    implementation("com.android.billingclient:billing-ktx:7.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
