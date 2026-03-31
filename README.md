# Rust UniFFI Gradle Plugin

Eliminates Rust/UniFFI boilerplate from Kotlin Multiplatform projects.

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

## Features

- **Auto cargo builds**: Builds Rust for desktop, Android (NDK), and WASM targets
- **UniFFI binding generation**: Generates Kotlin bindings for Android, JVM, and WASM
- **JNA integration**: Copies native libraries to JVM resources with platform detection
- **Convention-based**: Sensible defaults, fully overridable
- **KMP-native**: Wires into Kotlin Multiplatform plugin automatically

## Installation

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenCentral()
    }
}

// Version catalog (libs.versions.toml)
[plugins]
rustUniffi = { id = "io.github.mlm-games.rust-uniffi", version = "0.1.0" }
```

```kotlin
// shared/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.rustUniffi)
}

rustUniffi {
    libraryName.set("my_ffi")
    // Everything else uses conventions
}
```

## Usage

### Required Rust structure

```
rust/
├── Cargo.toml
├── src/lib.rs
├── uniffi-bindgen/Cargo.toml  (vendored uniffi-bindgen)
├── uniffi.android.toml
├── uniffi.jvm.toml
└── uniffi.wasm.toml
```

### Configuration options

```kotlin
rustUniffi {
    libraryName.set("mages_ffi")        // Required: crate name
    
    // Optional overrides:
    rustDir.set(rootProject.layout.projectDirectory.dir("rust"))
    cargoBin.set("cargo")
    androidAbis.set(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
    jniOutputDir.set(layout.projectDirectory.dir("src/androidMain/jniLibs"))
    
    // Extra JNA patterns (e.g. ONNX Runtime)
    jnaExtraPatterns.set(listOf("libonnxruntime*.so*"))
    jnaExtraDirs.set(listOf("deps"))
}
```

### CI override for specific ABI

```bash
./gradlew assembleDebug -PtargetAbi=arm64-v8a
```

## Publishing

```bash
./gradlew publishAndReleaseToMavenCentral
```

## License

Apache License 2.0 - See [LICENSE](LICENSE) for details.
