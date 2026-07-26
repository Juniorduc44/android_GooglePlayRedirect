plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.juniorduc44.playredirect"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.juniorduc44.playredirect"
        minSdk = 26
        targetSdk = 34
        versionCode = 10200
        versionName = "1.2.0"
    }

    val releaseKeystorePath = System.getenv("PLAY_REDIRECT_KEYSTORE")
        ?: System.getenv("PHP_USD_KEYSTORE") // optional reuse of local sideload keystore path
    val releaseStorePassword = System.getenv("PLAY_REDIRECT_STORE_PASSWORD")
        ?: System.getenv("PHP_USD_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("PLAY_REDIRECT_KEY_ALIAS")
        ?: System.getenv("PHP_USD_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("PLAY_REDIRECT_KEY_PASSWORD")
        ?: System.getenv("PHP_USD_KEY_PASSWORD")
    val hasReleaseSigning =
        !releaseKeystorePath.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
