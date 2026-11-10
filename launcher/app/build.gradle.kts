import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing comes from environment variables (set by CI from repository
// secrets) or from launcher/keystore.properties (never committed). When neither
// is present the release build falls back to the debug key, so local
// `assembleRelease` always works. See docs/SIGNING.md.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(prop)

val releaseStoreFile = signingValue("CAPITV_KEYSTORE_FILE", "storeFile")

android {
    namespace = "com.ekrtf.capitv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ekrtf.capitv"
        // 30 (Android 11) so the Nvidia Shield TV, which tops out at Android 11, is supported.
        minSdk = 30
        targetSdk = 36
        versionCode = (System.getenv("CAPITV_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("CAPITV_VERSION_NAME") ?: "0.1.0"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingValue("CAPITV_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("CAPITV_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("CAPITV_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = false
        // Version-bump nags are not actionable for a pinned, offline-by-design launcher.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.tv.material)
    implementation(libs.activity.compose)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
