plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.juniorduc44.phpusdconverter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.juniorduc44.phpusdconverter"
        minSdk = 26
        targetSdk = 34
        versionCode = 10603
        versionName = "1.6.3"
    }

    // Optional release signing via env vars (keystore not in git):
    // PHP_USD_KEYSTORE, PHP_USD_STORE_PASSWORD, PHP_USD_KEY_ALIAS, PHP_USD_KEY_PASSWORD
    val releaseKeystorePath = System.getenv("PHP_USD_KEYSTORE")
    val releaseStorePassword = System.getenv("PHP_USD_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("PHP_USD_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("PHP_USD_KEY_PASSWORD")
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
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
}
