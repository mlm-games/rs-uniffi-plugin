package org.mlm.rustuniffi

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class RustUniffiExtension @Inject constructor(project: Project) {

    private val objects  = project.objects
    private val layout   = project.layout
    private val rootDir  = project.rootProject.layout.projectDirectory
    private val providers = project.providers

    abstract val libraryName: Property<String>

    abstract val rustDir: DirectoryProperty

    abstract val cargoBin: Property<String>

    abstract val uniffiBindgenManifest: RegularFileProperty

    abstract val androidAbis: ListProperty<String>

    abstract val jniOutputDir: DirectoryProperty

    abstract val androidUniffiConfig: RegularFileProperty

    abstract val jvmUniffiConfig: RegularFileProperty

    abstract val jnaExtraPatterns: ListProperty<String>

    abstract val jnaExtraDirs: ListProperty<String>

    abstract val cargoNdkExtraArgs: ListProperty<String>

    abstract val wasmUniffiConfig: RegularFileProperty

    init {
        rustDir.convention(rootDir.dir("rust"))
        cargoBin.convention(PlatformUtil.cargoBin)
        uniffiBindgenManifest.convention(rustDir.file("uniffi-bindgen/Cargo.toml"))

        androidAbis.convention(
            providers.gradleProperty("targetAbi")
                .map { listOf(it) }
                .orElse(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
        )
        jniOutputDir.convention(layout.projectDirectory.dir("src/androidMain/jniLibs"))
        androidUniffiConfig.convention(rustDir.file("uniffi.android.toml"))

        jvmUniffiConfig.convention(rustDir.file("uniffi.jvm.toml"))
        jnaExtraPatterns.convention(emptyList())
        jnaExtraDirs.convention(emptyList())

        cargoNdkExtraArgs.convention(emptyList())

        wasmUniffiConfig.convention(rustDir.file("uniffi.wasm.toml"))
    }
}
