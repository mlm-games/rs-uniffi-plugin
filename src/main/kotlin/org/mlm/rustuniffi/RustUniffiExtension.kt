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

    abstract val cargoHome: Property<String>

    abstract val uniffiBindgenManifest: RegularFileProperty

    abstract val androidAbis: ListProperty<String>

    abstract val jniOutputDir: DirectoryProperty

    abstract val androidUniffiConfig: RegularFileProperty

    abstract val jvmUniffiConfig: RegularFileProperty

    abstract val jnaExtraPatterns: ListProperty<String>

    abstract val jnaExtraDirs: ListProperty<String>

    abstract val cargoNdkExtraArgs: ListProperty<String>

    init {
        rustDir.convention(rootDir.dir("rust"))
        libraryName.convention(
            providers.gradleProperty("ffiLibName").orElse(
                providers.provider {
                    val cargoToml = rustDir.get().file("Cargo.toml").asFile
                    PlatformUtil.crateNameFromCargoToml(cargoToml) ?: error(
                        "Cannot determine the Rust cdylib name. Set rustUniffi.libraryName, " +
                            "pass -PffiLibName=<name>, or declare [lib] name in $cargoToml"
                    )
                }
            )
        )
        cargoBin.convention(PlatformUtil.cargoBin)
        cargoHome.convention(providers.provider { System.getenv("CARGO_HOME") ?: "${System.getProperty("user.home")}/.cargo" })
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
    }
}
