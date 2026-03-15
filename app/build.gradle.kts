plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.github.jk1.dependency-license-report") version "2.5"
}

import java.io.FileInputStream
import java.util.Properties

android {
    namespace = "org.tomasino.stutter"
    compileSdk = 35

    val keystorePropsFile = rootProject.file("keystore.properties")
    val releaseSigningConfig = if (keystorePropsFile.exists()) {
        val keystoreProps = Properties().apply {
            FileInputStream(keystorePropsFile).use { load(it) }
        }
        signingConfigs.create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    } else {
        null
    }

    defaultConfig {
        applicationId = "org.tomasino.stutter"
        minSdk = 24
        targetSdk = 35
        versionCode = 27
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val screenshotUiMode = providers.gradleProperty("screenshotUiMode").orNull
        if (!screenshotUiMode.isNullOrBlank()) {
            testInstrumentationRunnerArguments["uiMode"] = screenshotUiMode
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningConfig != null) {
                signingConfig = releaseSigningConfig
            }
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

licenseReport {
    outputDir = layout.buildDirectory.dir("reports/dependency-license").get().asFile.path
    renderers = arrayOf(
        com.github.jk1.license.render.TextReportRenderer("licenses.txt"),
        com.github.jk1.license.render.JsonReportRenderer("licenses.json"),
    )
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
