package org.mlm.rustuniffi

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.mlm.rustuniffi.tasks.*
import java.io.File

private fun Project.extractBundledJnaRules(): File {
    val out = layout.buildDirectory.file("rust-uniffi/jna-consumer-rules.pro").get().asFile
    if (!out.exists()) {
        out.parentFile.mkdirs()
        RustUniffiPlugin::class.java.classLoader
            .getResourceAsStream("jna-consumer-rules.pro")
            .use { input ->
                requireNotNull(input) { "Missing bundled resource jna-consumer-rules.pro" }
                out.outputStream().use { input.copyTo(it) }
            }
    }
    return out
}

private fun Project.configureJnaConsumerRules() {
    val rulesFile = extractBundledJnaRules()

    pluginManager.withPlugin("com.android.library") {
        extensions.getByType(LibraryExtension::class.java)
            .defaultConfig
            .consumerProguardFiles(rulesFile)
    }

    pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
        val kmp = extensions.getByType(KotlinMultiplatformExtension::class.java)
        kmp.targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach {
            optimization.consumerKeepRules.apply {
                publish = true
                file(rulesFile)
            }
        }
    }
}

class RustUniffiPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val ext = project.extensions.create("rustUniffi", RustUniffiExtension::class.java)

        val hostLibName = ext.libraryName.map { PlatformUtil.hostLibName(it) }
        val hostLibFile = ext.rustDir.zip(hostLibName) { dir, name ->
            dir.file("target/release/$name").asFile
        }
        val hostLibRegularFile = ext.rustDir.flatMap { dir ->
            hostLibName.map { name -> dir.file("target/release/$name") }
        }

        val uniffiAndroidOut = project.layout.buildDirectory.dir("generated/uniffi/androidMain/kotlin")
        val uniffiJvmOut     = project.layout.buildDirectory.dir("generated/uniffi/jvmMain/kotlin")
        val uniffiWasmOut    = project.layout.buildDirectory.dir("generated/uniffi/wasmJsMain/kotlin")

        val cargoBuildDesktop = project.tasks.register("cargoBuildDesktop", CargoBuildTask::class.java) {
            cargoBin.set(ext.cargoBin)
            rustProjectDir.set(ext.rustDir)
        }

        val cargoBuildAndroid = project.tasks.register("cargoBuildAndroid", CargoNdkTask::class.java) {
            abis.set(ext.androidAbis)
            cargoBin.set(ext.cargoBin)
            rustProjectDir.set(ext.rustDir)
            jniOut.set(ext.jniOutputDir)
            extraArgs.set(ext.cargoNdkExtraArgs)
        }

        val cargoBuildWasm = project.tasks.register("cargoBuildWasm", CargoBuildWasmTask::class.java) {
            cargoBin.set(ext.cargoBin)
            rustProjectDir.set(ext.rustDir)
        }

        val genUniFFIAndroid = project.tasks.register("genUniFFIAndroid", GenerateUniFFITask::class.java) {
            dependsOn(cargoBuildDesktop)
            libraryFile.set(hostLibRegularFile)
            configFile.set(ext.androidUniffiConfig)
            language.set("kotlin")
            cargoBin.set(ext.cargoBin)
            vendoredManifest.set(ext.uniffiBindgenManifest)
            outDir.set(uniffiAndroidOut)
        }

        val genUniFFIJvm = project.tasks.register("genUniFFIJvm", GenerateUniFFITask::class.java) {
            dependsOn(cargoBuildDesktop)
            libraryFile.set(hostLibRegularFile)
            configFile.set(ext.jvmUniffiConfig)
            language.set("kotlin")
            cargoBin.set(ext.cargoBin)
            vendoredManifest.set(ext.uniffiBindgenManifest)
            outDir.set(uniffiJvmOut)
        }

        val genUniFFIWasm = project.tasks.register("genUniFFIWasm", GenerateUniFFITask::class.java) {
            dependsOn(cargoBuildDesktop)
            libraryFile.set(hostLibRegularFile)
            configFile.set(ext.wasmUniffiConfig)
            language.set("kotlin")
            cargoBin.set(ext.cargoBin)
            vendoredManifest.set(ext.uniffiBindgenManifest)
            outDir.set(uniffiWasmOut)
        }

        val copyNativeForJna = project.tasks.register("copyNativeForJna", Copy::class.java) {
            dependsOn(cargoBuildDesktop)

            into(project.file("src/jvmMain/resources/${PlatformUtil.jnaPlatformDir}"))

            from(ext.rustDir.map { it.dir("target/release") }) {
                include(hostLibName.get())
                ext.jnaExtraPatterns.get().forEach { include(it) }
            }

            ext.jnaExtraDirs.get().forEach { subdir ->
                from(ext.rustDir.map { it.dir("target/release/$subdir") }) {
                    ext.jnaExtraPatterns.get().forEach { include(it) }
                }
            }
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kmpExt = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            kmpExt.sourceSets.configureEach {
                when (name) {
                    "androidMain" -> kotlin.srcDir(uniffiAndroidOut)
                    "jvmMain"     -> kotlin.srcDir(uniffiJvmOut)
                    "wasmJsMain"  -> kotlin.srcDir(uniffiWasmOut)
                }
            }

            kmpExt.targets.configureEach {
                when (platformType) {
                    KotlinPlatformType.androidJvm -> {
                        compilations.configureEach {
                            compileTaskProvider.configure {
                                dependsOn(genUniFFIAndroid, cargoBuildAndroid)
                            }
                        }
                    }
                    KotlinPlatformType.jvm -> {
                        compilations.configureEach {
                            compileTaskProvider.configure {
                                dependsOn(genUniFFIJvm)
                            }
                        }
                    }
                    KotlinPlatformType.wasm -> {
                        compilations.configureEach {
                            compileTaskProvider.configure {
                                dependsOn(genUniFFIWasm, cargoBuildWasm)
                            }
                        }
                    }
                    else -> { }
                }
            }

            project.tasks.matching { it.name == "jvmProcessResources" }.configureEach {
                dependsOn(copyNativeForJna)
            }

            project.tasks.matching {
                it.name.contains("JniLibFolders") && it.name.contains("AndroidMain", ignoreCase = true)
            }.configureEach {
                dependsOn(cargoBuildAndroid)
            }
            project.tasks.matching {
                it.name == "mergeAndroidMainJniLibFolders"
            }.configureEach {
                dependsOn(cargoBuildAndroid)
            }

            project.tasks.matching { it.name.startsWith("kspAndroid") }.configureEach {
                dependsOn(genUniFFIAndroid)
            }
            project.tasks.matching {
                it.name.startsWith("kspJvm") || it.name == "kspKotlinJvm"
            }.configureEach {
                dependsOn(genUniFFIJvm)
            }
            project.tasks.matching {
                it.name.startsWith("kspWasmJs") || it.name == "kspKotlinWasmJs"
            }.configureEach {
                dependsOn(genUniFFIWasm)
            }

            project.configureJnaConsumerRules()
        }
    }
}
