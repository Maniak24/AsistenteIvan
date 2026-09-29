set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -B/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/33 -L/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android/33 -L/data/data/com.termux/files/usr/lib/clang/21/lib/linux/aarch64")
cmake_minimum_required(VERSION 3.31)


# Toolchain ARM64 Termux + NDK
set(NDK_BASE "/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64")

# CMake/NDK debe encontrar libc++_shared durante la prueba del compilador

set(CMAKE_EXE_LINKER_FLAGS_INIT
    "-L${NDK_BASE}/sysroot/usr/lib/aarch64-linux-android/33 -L${NDK_BASE}/sysroot/usr/lib/aarch64-linux-android -L/data/data/com.termux/files/usr/lib/clang/21/lib/linux/aarch64"
)

set(CMAKE_SHARED_LINKER_FLAGS_INIT
    "-L${NDK_BASE}/sysroot/usr/lib/aarch64-linux-android/33 -L${NDK_BASE}/sysroot/usr/lib/aarch64-linux-android -L/data/data/com.termux/files/usr/lib/clang/21/lib/linux/aarch64"
)

project("ai-chat" VERSION 1.0.0 LANGUAGES C CXX)

# Runtime ARM64 de Clang 21 + bibliotecas Termux
add_link_options(
    "-L/data/data/com.termux/files/usr/lib"
    "-L/data/data/com.termux/files/usr/lib/clang/21/lib/linux"
    "/data/data/com.termux/files/usr/lib/clang/21/lib/linux/libclang_rt.builtins-aarch64-android.a"
    "/data/data/com.termux/files/usr/lib/libunwind.a"
)


set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED true)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED true)

set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}"   CACHE STRING "" FORCE)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}" CACHE STRING "" FORCE)

# --------------------------------------------------------------------------
# AI Chat library
# --------------------------------------------------------------------------

if(DEFINED ANDROID_ABI)
    message(STATUS "Detected Android ABI: ${ANDROID_ABI}")
    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(GGML_SYSTEM_ARCH "ARM")
        set(GGML_CPU_KLEIDIAI ON)
        set(GGML_OPENMP ON)
    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(GGML_SYSTEM_ARCH "x86")
        set(GGML_CPU_KLEIDIAI OFF)
        set(GGML_OPENMP OFF)
    else()
        message(FATAL_ERROR "Unsupported ABI: ${ANDROID_ABI}")
    endif()
endif()

set(LLAMA_SRC ${CMAKE_CURRENT_LIST_DIR}/../../../../llama.cpp)
add_subdirectory(${LLAMA_SRC} build-llama)

add_library(${CMAKE_PROJECT_NAME} SHARED
        ai_chat.cpp)

target_compile_definitions(${CMAKE_PROJECT_NAME} PRIVATE
        GGML_SYSTEM_ARCH=${GGML_SYSTEM_ARCH}
        GGML_CPU_KLEIDIAI=$<BOOL:${GGML_CPU_KLEIDIAI}>
        GGML_OPENMP=$<BOOL:${GGML_OPENMP}>
)

target_include_directories(${CMAKE_PROJECT_NAME} PRIVATE
        ${LLAMA_SRC}
        ${LLAMA_SRC}/common
        ${LLAMA_SRC}/include
        ${LLAMA_SRC}/ggml/include
        ${LLAMA_SRC}/ggml/src)

target_link_libraries(${CMAKE_PROJECT_NAME}
        llama
        llama-common
        android
        log)

set(CMAKE_C_COMPILER "/data/data/com.termux/files/usr/bin/clang" CACHE FILEPATH "" FORCE)
set(CMAKE_CXX_COMPILER "/data/data/com.termux/files/usr/bin/clang++" CACHE FILEPATH "" FORCE)
set(CMAKE_C_FLAGS "--target=aarch64-linux-android33 --sysroot=/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/sysroot" CACHE STRING "" FORCE)
set(CMAKE_CXX_FLAGS "--target=aarch64-linux-android33 --sysroot=/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64/sysroot" CACHE STRING "" FORCE)

# Rutas necesarias para el toolchain ARM64 de Termux + NDK
set(NDK_BASE "/data/data/com.termux/files/usr/var/lib/proot-distro/containers/debian/rootfs/root/android-sdk/ndk/29.0.13113456/toolchains/llvm/prebuilt/linux-x86_64")

link_directories(
    "${NDK_BASE}/sysroot/usr/lib/aarch64-linux-android"
    "${NDK_BASE}/sysroot/usr/lib/aarch64-linux-android/33"
    "/data/data/com.termux/files/usr/lib/clang/21/lib/linux/aarch64"
)
