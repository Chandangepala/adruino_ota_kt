plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.android.adruino_ota_kt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.adruino_ota_kt"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.core.ktx.v1150) // Or newer
    implementation(libs.androidx.activity.compose.v172) // Or newer
    implementation(libs.ui) // Or newer
    implementation(libs.androidx.material) // Or newer
    implementation(libs.ui.tooling.preview) // Or newer
    implementation(libs.androidx.lifecycle.runtime.ktx.v287) // Or newer
    implementation(libs.androidx.documentfile) // for file access
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    //library to read serial port
    implementation ("com.github.mik3y:usb-serial-for-android:3.4.2")
    // File picker
    implementation ("com.google.accompanist:accompanist-permissions:0.34.0")


}