import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.notificationhistory"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.notificationhistory"
        minSdk = 33
        targetSdk = 36
        versionCode = (project.findProperty("VERSION_CODE") as? String)?.toIntOrNull() ?: 1
        // x-release-please-start-version
        val baseVersionName = "1.2.0"
        // x-release-please-end
        versionName = (project.findProperty("VERSION_NAME") as? String) ?: baseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreProperties = Properties().apply {
                val keystorePropertiesFile = rootProject.file("keystore.properties")
                if (keystorePropertiesFile.exists()) {
                    keystorePropertiesFile.inputStream().use { load(it) }
                }
            }

            val localProperties = Properties().apply {
                val localPropertiesFile = rootProject.file("local.properties")
                if (localPropertiesFile.exists()) {
                    localPropertiesFile.inputStream().use { load(it) }
                }
            }

            fun resolveValue(name: String): String? =
                System.getenv(name)
                    ?: (project.findProperty(name) as? String)
                    ?: keystoreProperties.getProperty(name)
                    ?: localProperties.getProperty(name)

            val keystorePath = resolveValue("KEYSTORE_FILE")
            val keystoreFile = keystorePath?.takeIf { it.isNotBlank() }?.let { path ->
                rootProject.file(path).takeIf { it.exists() } ?: file(path).takeIf { it.exists() }
            }
            if (keystoreFile != null) {
                storeFile = keystoreFile
                storePassword = resolveValue("KEYSTORE_PASSWORD")
                keyAlias = resolveValue("KEY_ALIAS")
                keyPassword = resolveValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle & ViewModel Compose
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
