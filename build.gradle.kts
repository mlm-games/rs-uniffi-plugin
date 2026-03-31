@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.vanniktech.maven.publish)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("rustUniffiPlugin") {
            id = "io.github.mlm-games.rust-uniffi"
            implementationClass = "org.mlm.rustuniffi.RustUniffiPlugin"
            tags.set(listOf("android", "rust", "uniffi", "kotlin-multiplatform", "jvm", "wasm"))
        }
    }
}
