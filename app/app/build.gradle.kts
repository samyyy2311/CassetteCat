import java.util.Properties
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "in.caffeinelabs.cassettecat"
    compileSdk = 37
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    val appVersionName = System.getenv("GITHUB_REF_NAME")?.takeIf { it.startsWith("v") }?.removePrefix("v")
        ?: (project.findProperty("versionName") as? String)
        ?: "1.6.0"

    val appVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull()
        ?: (project.findProperty("versionCode") as? String)?.toIntOrNull()
        ?: 10

    defaultConfig {
        applicationId = "in.caffeinelabs.cassettecat"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        create("release") {
            // Load from keystore.properties file if it exists (local dev)
            val keystoreProps = Properties()
            val keystoreFile = rootProject.file("keystore.properties")
            if (keystoreFile.exists()) keystoreProps.load(keystoreFile.inputStream())

            val keystorePath = System.getenv("KEYSTORE_FILE")
                ?: keystoreProps.getProperty("RELEASE_STORE_FILE")
                ?: (project.findProperty("RELEASE_STORE_FILE") as? String)
            if (!keystorePath.isNullOrEmpty() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: keystoreProps.getProperty("RELEASE_STORE_PASSWORD")
                    ?: (project.findProperty("RELEASE_STORE_PASSWORD") as? String) ?: ""
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: keystoreProps.getProperty("RELEASE_KEY_ALIAS")
                    ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String) ?: ""
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: keystoreProps.getProperty("RELEASE_KEY_PASSWORD")
                    ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String) ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.getByName("release")
            val hasProductionSigning = releaseSigning.storeFile?.exists() == true &&
                !releaseSigning.storePassword.isNullOrBlank() &&
                !releaseSigning.keyAlias.isNullOrBlank() &&
                !releaseSigning.keyPassword.isNullOrBlank()
            isProfileable = hasProductionSigning
            if (hasProductionSigning) {
                signingConfig = releaseSigning
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.icons.lucide.android)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
}
