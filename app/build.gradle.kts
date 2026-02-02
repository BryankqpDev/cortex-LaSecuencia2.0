plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.Cortex_LaSecuencia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.Cortex_LaSecuencia"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true // ← NUEVO: Necesario para BiometriaActivity
    }

    // ========================================
    // NUEVO: Configuración para TensorFlow Lite
    // ========================================
    androidResources {
        noCompress +="tflite"
    }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                "src/main/res_icons",
                "src/main/res_buttons",
                "src/main/res_backgrounds"
            )
        }
    }
}

dependencies {
    // ========================================
    // CORE ANDROID (TUS DEPENDENCIAS ACTUALES)
    // ========================================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")

    // ========================================
    // FIREBASE (TUS DEPENDENCIAS ACTUALES)
    // ========================================
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    // NUEVO: Firestore para almacenar embeddings biométricos
    implementation("com.google.firebase:firebase-firestore-ktx")

    // ========================================
    // BIOMETRÍA BÁSICA (YA LO TIENES)
    // ========================================
    implementation("androidx.biometric:biometric:1.1.0")

    // ========================================
    // CAMERAХ (TUS DEPENDENCIAS ACTUALES)
    // ========================================
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ========================================
    // ML KIT FACE DETECTION (YA LO TIENES - ACTUALIZADO)
    // ========================================
    // Nota: Ya tienes face.detection, verificar que sea la versión más reciente
    implementation(libs.face.detection) // Verificar que sea >= 16.1.6
    // O agregar explícitamente si no está en libs:
    // implementation("com.google.mlkit:face-detection:16.1.6")

    // ========================================
    // NUEVO: TENSORFLOW LITE (PARA FACENET Y LIVENESS)
    // ========================================
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // Opcional: Aceleración GPU (comentar si causa problemas)
    // implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // ========================================
    // COROUTINES (NUEVO - Para procesamiento asíncrono)
    // ========================================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ========================================
    // EXCEL Y PDF (TUS DEPENDENCIAS ACTUALES)
    // ========================================
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:itextg:5.5.10")

    // ========================================
    // OTRAS UTILIDADES (TUS DEPENDENCIAS ACTUALES)
    // ========================================
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.4")

    // ========================================
    // TESTING (TUS DEPENDENCIAS ACTUALES)
    // ========================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}