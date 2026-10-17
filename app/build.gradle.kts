plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.roborazzi)
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    autoCorrect = false
}

android {
    namespace = "com.example.lev"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.lev"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = false
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    tasks.withType<Test> {
        testLogging {
            showStandardStreams = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
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
    implementation(libs.androidx.navigation.compose)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // OkHttp & Moshi
    implementation(libs.okhttp)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)

    // Sincronização em segundo plano (M7): debounce + periódica
    implementation(libs.work.runtime.ktx)

    // java.time and other JDK APIs on minSdk 24
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    // Screenshots do design system (claro/escuro/fonte grande)
    testImplementation(composeBom)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.robolectric)
}

// Referências dos screenshots versionadas (sobrevivem ao clean)
roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}
