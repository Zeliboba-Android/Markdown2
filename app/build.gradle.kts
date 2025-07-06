plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.markdown2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.markdown2"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Добавьте это для решения конфликтов
    configurations.all {
        resolutionStrategy {
            force("com.google.guava:guava:32.1.3-android")
            exclude(group = "com.google.guava", module = "listenablefuture")
            exclude(group = "org.hamcrest", module = "hamcrest-core")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.testng)

    testImplementation(libs.junit)
    testImplementation(libs.hamcrest)

    androidTestImplementation(libs.androidx.espresso.core) {
        exclude(group = "org.hamcrest")
    }

    implementation(libs.guava) {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
}