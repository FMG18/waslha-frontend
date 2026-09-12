plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.waslha.captain"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.waslha.captain"
        minSdk = 26
        targetSdk = 35
        versionCode = 102
        versionName = "1.0.2"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("WASLHA_KEYSTORE_PATH")
            val keystorePassword = System.getenv("WASLHA_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("WASLHA_KEY_ALIAS")
            val keyPassword = System.getenv("WASLHA_KEY_PASSWORD")

            require(!keystorePath.isNullOrBlank()) { "WASLHA_KEYSTORE_PATH is missing" }
            require(!keystorePassword.isNullOrBlank()) { "WASLHA_KEYSTORE_PASSWORD is missing" }
            require(!keyAlias.isNullOrBlank()) { "WASLHA_KEY_ALIAS is missing" }
            require(!keyPassword.isNullOrBlank()) { "WASLHA_KEY_PASSWORD is missing" }

            val keystoreFile = file(keystorePath)
            require(keystoreFile.isFile) { "Captain release keystore not found: ${keystoreFile.absolutePath}" }

            storeFile = keystoreFile
            storePassword = keystorePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    sourceSets["main"].java.exclude(
        "**/CaptainActivity.kt",
        "**/CaptainEarningsV2.kt"
    )
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}