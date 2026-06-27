plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.mpa.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.mpa.client"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.5.0"

        val activationApi = project.findProperty("MPA_ACTIVATION_API")?.toString()
            ?: System.getenv("MPA_ACTIVATION_API")
            ?: ""
        buildConfigField("String", "MPA_ACTIVATION_API", "\"$activationApi\"")

        ndk {
            // Включаем только мобильные архитектуры. 
            // x86 и x86_64 нужны только для эмуляторов и весят много.
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            // Включаем оптимизацию даже в дебаге, если нужно потестить размер
            isMinifyEnabled = false 
        }
    }

    bundle {
        abi {
            enableSplit = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.coroutines.android)
    implementation(libs.workmanager)

    // CameraX + ML Kit — QR scanner
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)

    // sing-box libbox — локальный AAR
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    debugImplementation(libs.compose.ui.tooling)
}
