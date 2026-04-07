plugins {
    alias(libs.plugins.android.application)

}

android {
    namespace = "com.example.lacetastic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lacetastic"
        minSdk = 26
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)


    // Rajawali 3D Library
    implementation("org.rajawali3d:rajawali:1.1.970")

    // Networking — OkHttp (used by LanyardApiService)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")



    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
