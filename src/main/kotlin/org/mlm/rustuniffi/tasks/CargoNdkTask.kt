package org.mlm.rustuniffi.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class CargoNdkTask @Inject constructor(
    private val execOps: ExecOperations,
) : DefaultTask() {

    @get:Input
    abstract val abis: ListProperty<String>

    @get:Input
    abstract val cargoBin: Property<String>

    @get:InputDirectory
    abstract val rustProjectDir: DirectoryProperty

    @get:OutputDirectory
    abstract val jniOut: DirectoryProperty

    @get:Input
    abstract val extraArgs: ListProperty<String>

    @TaskAction
    fun build() {
        val rustDir = rustProjectDir.get().asFile
        val outDir  = jniOut.get().asFile.also { it.mkdirs() }

        abis.get().forEach { abi ->
            execOps.exec {
                workingDir = rustDir
                commandLine(
                    buildList {
                        add(cargoBin.get())
                        add("ndk")
                        addAll(listOf("-t", abi))
                        addAll(listOf("-o", outDir.absolutePath))
                        addAll(extraArgs.getOrElse(emptyList()))
                        addAll(listOf("build", "--release"))
                    }
                )
            }
        }
    }
}
