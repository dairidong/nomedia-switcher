plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
}

val releaseStoreFile = providers.environmentVariable("NOMEDIA_RELEASE_STORE_FILE")
val releaseStorePassword = providers.environmentVariable("NOMEDIA_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("NOMEDIA_RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("NOMEDIA_RELEASE_KEY_PASSWORD")
val releaseSigningEnv = mapOf(
    "NOMEDIA_RELEASE_STORE_FILE" to releaseStoreFile.orNull,
    "NOMEDIA_RELEASE_STORE_PASSWORD" to releaseStorePassword.orNull,
    "NOMEDIA_RELEASE_KEY_ALIAS" to releaseKeyAlias.orNull,
    "NOMEDIA_RELEASE_KEY_PASSWORD" to releaseKeyPassword.orNull,
)
val missingReleaseSigningEnv = releaseSigningEnv
    .filterValues { it.isNullOrBlank() }
    .keys
    .sorted()
val hasReleaseSigningConfig = missingReleaseSigningEnv.isEmpty()
val isReleaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("release", ignoreCase = true)
}

android {
    namespace = "com.nomedia.switcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nomedia.switcher"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "zh-rCN")
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    testImplementation(libs.junit4)
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.work:work-testing:2.9.1")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.room:room-testing:2.6.1")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    ksp("androidx.room:room-compiler:2.6.1")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

if (isReleaseTaskRequested && !hasReleaseSigningConfig) {
    throw GradleException(
        "Missing release signing environment variables: ${missingReleaseSigningEnv.joinToString(", ")}. " +
            "Set these before running a release build.",
    )
}
