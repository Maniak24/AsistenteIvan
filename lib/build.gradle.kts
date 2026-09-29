plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.arm.aichat"
    compileSdk = 36

    ndkVersion = "29.0.13113456"

    defaultConfig {
        minSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ndk {
             abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                arguments += "-DANDROID_STL=c++_shared"
                arguments += "-DCMAKE_HAVE_LIBC_PTHREAD=1"
                arguments += "-DCMAKE_THREAD_LIBS_INIT=pthread"
                arguments += "-DCMAKE_USE_PTHREADS_INIT=1"
                arguments += "-DCMAKE_C_COMPILER=/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
                arguments += "-DCMAKE_CXX_COMPILER=/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/bin/clang++"
                arguments += "-DCMAKE_C_COMPILER_TARGET=aarch64-none-linux-android33"
                arguments += "-DCMAKE_CXX_COMPILER_TARGET=aarch64-none-linux-android33"
                arguments += "-DCMAKE_EXE_LINKER_FLAGS=-L/data/data/com.termux/files/usr/lib -L/data/data/com.termux/files/usr/lib/clang/21/lib/linux/aarch64 -L/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/33 -L/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android "
                arguments += "-DCMAKE_SHARED_LINKER_FLAGS=-L/data/data/com.termux/files/usr/lib -L/data/data/com.termux/files/usr/lib/clang/21/lib/linux/aarch64 -L/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/33 -L/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android "
                arguments += "-DCMAKE_MESSAGE_LOG_LEVEL=DEBUG"
                arguments += "-DCMAKE_VERBOSE_MAKEFILE=ON"

                arguments += "-DBUILD_SHARED_LIBS=ON"
                arguments += "-DLLAMA_BUILD_APP=OFF"
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DLLAMA_OPENSSL=OFF"

                arguments += "-DGGML_NATIVE=OFF"
                arguments += "-DGGML_BACKEND_DL=ON"
                arguments += "-DGGML_CPU_ALL_VARIANTS=ON"
                arguments += "-DGGML_LLAMAFILE=OFF"
            }
        }
        aarMetadata {
            minCompileSdk = 35
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "4.3.4"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)

        compileOptions {
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    publishing {
        singleVariant("release") {
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
