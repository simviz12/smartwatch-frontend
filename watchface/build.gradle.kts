// Pulso watch face — Watch Face Format (resource-only APK, no Kotlin code).
// Required by Wear OS 5+: watch faces must be declared in XML, not drawn by app code.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.pulso.watchface"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pulso.watchface"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
