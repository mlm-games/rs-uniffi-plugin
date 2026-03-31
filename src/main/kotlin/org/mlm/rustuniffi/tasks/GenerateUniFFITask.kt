package org.mlm.rustuniffi.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class GenerateUniFFITask @Inject constructor(
    private val execOps: ExecOperations,
) : DefaultTask() {

    @get:InputFile
    abstract val libraryFile: RegularFileProperty

    @get:Optional
    @get:InputFile
    abstract val configFile: RegularFileProperty

    @get:Input
    abstract val language: Property<String>

    @get:Input
    abstract val cargoBin: Property<String>

    @get:InputFile
    abstract val vendoredManifest: RegularFileProperty

    @get:OutputDirectory
    abstract val outDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val manifest = vendoredManifest.orNull?.asFile
            ?: throw GradleException("uniffi-bindgen manifest is not set")
        val lib = libraryFile.get().asFile

        val cmd = mutableListOf(
            cargoBin.get(), "run", "--release",
            "--manifest-path", manifest.absolutePath,
            "--bin", "uniffi-bindgen",
            "--",
            "generate",
            "--library", lib.absolutePath,
            "--language", language.get(),
            "--out-dir", outDir.get().asFile.absolutePath,
        )

        configFile.orNull?.asFile?.let { cfg ->
            cmd += listOf("--config", cfg.absolutePath)
        }

        outDir.get().asFile.mkdirs()
        execOps.exec {
            workingDir = manifest.parentFile
            commandLine(cmd)
        }
    }
}
