fun projectOrEnv(name: String): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orElse("").get()


fun projectOrEnv(name: String): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orElse("").get()


plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
}

android {
    namespace = "com.sidequests.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sidequests.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"

    buildConfigField("String", "SUPABASE_URL", "\"${projectOrEnv("SUPABASE_URL")}\"")
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${projectOrEnv("SUPABASE_PUBLISHABLE_KEY")}\"")
    buildConfigField("String", "SUPABASE_QUEST_PROOFS_BUCKET", "\"${projectOrEnv("SUPABASE_QUEST_PROOFS_BUCKET")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sidequests.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sidequests.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.2.0"

    buildConfigField("String", "SUPABASE_URL", "\"${projectOrEnv("SUPABASE_URL")}\"")
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${projectOrEnv("SUPABASE_PUBLISHABLE_KEY")}\"")
    buildConfigField("String", "SUPABASE_QUEST_PROOFS_BUCKET", "\"${projectOrEnv("SUPABASE_QUEST_PROOFS_BUCKET")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
