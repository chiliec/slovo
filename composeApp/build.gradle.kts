import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    // No iosX64: Compose Multiplatform stopped publishing Intel-simulator
    // artifacts as of 1.11, and every supported build host is Apple Silicon.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.sqldelight.coroutines)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.sqldelight.jvm)
        }
        androidUnitTest.dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.androidx.activity.compose)
        }
    }
}

sqldelight {
    databases {
        register("SlovoDatabase") { packageName.set("cx.viz.slovo.db") }
    }
}

// Release signing is driven by a gitignored keystore.properties at the repo root
// (see keystore.properties.example). Absent it, release builds are left unsigned
// so CI and clean checkouts still assemble.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

// Play requires a strictly increasing versionCode per upload. The release lanes
// derive the next one from the highest code already on Play and pass it in via
// -PversionCode (or ANDROID_VERSION_CODE); everything else — local debug builds,
// CI unit tests, clean checkouts — falls back to 1.
val resolvedVersionCode = (
    findProperty("versionCode") as String? ?: System.getenv("ANDROID_VERSION_CODE")
)?.trim()?.takeIf { it.isNotEmpty() }?.let {
    it.toIntOrNull() ?: throw GradleException("versionCode must be an integer, got \"$it\"")
} ?: 1

android {
    namespace = "cx.viz.slovo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    defaultConfig {
        applicationId = "cx.viz.slovo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = resolvedVersionCode
        versionName = "1.0.0"
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric's classloader disrupts DriverManager's lazy ServiceLoader
        // discovery of the SQLite JDBC driver; loading it via the jdbc.drivers
        // system property registers it deterministically at DriverManager init.
        unitTests.all { it.systemProperty("jdbc.drivers", "org.sqlite.JDBC") }
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies { debugImplementation(compose.uiTooling) }
}
