plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    buildFeatures {
        buildConfig = true
    }


    namespace = "com.coloniavictoria.municipal"
    compileSdk = 36

    defaultConfig {
        fun configValue(name: String): String {
            val localProperties = rootProject.file("local.properties")

            val localValue = if (localProperties.exists()) {
                java.util.Properties().apply {
                    localProperties.inputStream().use { load(it) }
                }.getProperty(name)
            } else {
                null
            }

            return project.findProperty(name)?.toString()
                ?: System.getenv(name)
                ?: localValue
                ?: ""
        }

        fun buildConfigString(value: String): String =
            "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

        val supabaseUrl = configValue("SUPABASE_URL")
        val supabaseKey = configValue("SUPABASE_PUBLISHABLE_KEY")

        buildConfigField(
            "String",
            "SUPABASE_URL",
            buildConfigString(supabaseUrl)
        )
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            buildConfigString(supabaseKey)
        )

        applicationId = "com.coloniavictoria.municipal"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:3.0.3")
implementation(files("libs/sherpa-onnx-1.13.8.aar"))
    implementation(libs.bundles.androidx)
    implementation(libs.material)

    implementation(project(":lib"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
