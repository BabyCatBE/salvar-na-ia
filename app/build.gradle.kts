plugins {
    id("com.android.application")
}

android {
    namespace = "com.babycatbe.salvarnaia"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.babycatbe.salvarnaia"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
