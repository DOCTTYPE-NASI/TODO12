plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.nasi.organizer"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.nasi.organizer"
        minSdk = 26
        targetSdk = 34
        // Jede Version aus GitHub Actions bekommt eine höhere Nummer, damit Updates sich drüber installieren lassen
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    // Fester Schlüssel im Repo: so lassen sich neue APKs über die alte installieren, ohne Daten zu verlieren
    signingConfigs {
        create("fixed") {
            storeFile = file("organizer.jks")
            storePassword = "organizer"
            keyAlias = "organizer"
            keyPassword = "organizer"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
}
