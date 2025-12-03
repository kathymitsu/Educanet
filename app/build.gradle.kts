plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.educanet"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.educanet"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    buildFeatures { compose = true }
}

dependencies {
    // --- Compose / Material 3 (tu setup actual) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.12")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.animation:animation:1.7.4")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation(libs.androidx.compose.foundation)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.material3) // Ya estaba aquí, movida.


    testImplementation(libs.junit)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    // 🔥 CORRECCIÓN CRUCIAL: Añadir soporte para Coroutines y Tareas de Play Services.
    // Esto es clave para que las operaciones asíncronas de Firebase como putFile()
    // y la gestión de la URI en el hilo de la UI no fallen silenciosamente.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutin")
}
