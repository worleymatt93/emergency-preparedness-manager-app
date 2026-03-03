plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.emergencypreparednessmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.emergencypreparednessmanager"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export: app/schemas/...
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.preference)

    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Local unit tests
    testImplementation(libs.junit)

    // Instrumented tests
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.espresso.core)

    // Room testing (instrumented)
    androidTestImplementation(libs.room.testing)

    // Annotation processing for tests (safe to include)
    testAnnotationProcessor(libs.room.compiler)
    androidTestAnnotationProcessor(libs.room.compiler)
}