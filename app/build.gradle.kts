import java.util.Properties

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()

if (versionPropsFile.exists()) {
    versionProps.load(versionPropsFile.inputStream())
} else {
    throw GradleException("version.properties not found!")
}

val versionMajor = versionProps["VERSION_MAJOR"].toString().toInt()
val versionMinor = versionProps["VERSION_MINOR"].toString().toInt()
val versionPatch = versionProps["VERSION_PATCH"].toString().toInt()
val versionCodeProp = versionProps["VERSION_CODE"].toString().toInt()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.masjid.tvsholat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.masjid.tvsholat"
        minSdk = 24
        targetSdk = 36
        versionCode = versionCodeProp
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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
        buildConfig = true
    }
}

dependencies {
 
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Hisab sholat (metode Kemenag)
    implementation("com.batoulapps.adhan:adhan:1.2.1")

    // NanoHTTPD (admin via HP)
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // DataStore (config lokal)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    // Image Loading (Coil)
    implementation(libs.coil)
    
    // QR Code Generation
    implementation("com.google.zxing:core:3.5.3")
}

tasks.register("bumpPatch") {
    group = "versioning"
    description = "Bump PATCH version"

    doLast {
        val props = Properties()
        props.load(versionPropsFile.inputStream())

        val major = props["VERSION_MAJOR"].toString().toInt()
        val minor = props["VERSION_MINOR"].toString().toInt()
        val patch = props["VERSION_PATCH"].toString().toInt() + 1
        val code = props["VERSION_CODE"].toString().toInt() + 1

        props["VERSION_PATCH"] = patch.toString()
        props["VERSION_CODE"] = code.toString()

        props.store(versionPropsFile.writer(), null)

        println("✔ Version bumped to $major.$minor.$patch ($code)")
    }
}

tasks.register("bumpMinor") {
    group = "versioning"
    description = "Bump MINOR version"

    doLast {
        val props = Properties()
        props.load(versionPropsFile.inputStream())

        val major = props["VERSION_MAJOR"].toString().toInt()
        val minor = props["VERSION_MINOR"].toString().toInt() + 1
        val code = props["VERSION_CODE"].toString().toInt() + 10

        props["VERSION_MINOR"] = minor.toString()
        props["VERSION_PATCH"] = "0"
        props["VERSION_CODE"] = code.toString()

        props.store(versionPropsFile.writer(), null)

        println("✔ Version bumped to $major.$minor.0 ($code)")
    }
}

tasks.register("bumpMajor") {
    group = "versioning"
    description = "Bump MAJOR version"

    doLast {
        val props = Properties()
        props.load(versionPropsFile.inputStream())

        val major = props["VERSION_MAJOR"].toString().toInt() + 1
        val code = props["VERSION_CODE"].toString().toInt() + 100

        props["VERSION_MAJOR"] = major.toString()
        props["VERSION_MINOR"] = "0"
        props["VERSION_PATCH"] = "0"
        props["VERSION_CODE"] = code.toString()

        props.store(versionPropsFile.writer(), null)

        println("✔ Version bumped to $major.0.0 ($code)")
    }
}
