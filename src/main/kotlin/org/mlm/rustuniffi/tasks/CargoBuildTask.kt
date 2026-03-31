package org.mlm.rustuniffi.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class CargoBuildTask @Inject constructor(
    private val execOps: ExecOperations,
) : DefaultTask() {

    @get:Input
    abstract val cargoBin: Property<String>

    @get:InputDirectory
    abstract val rustProjectDir: DirectoryProperty

    @TaskAction
    fun build() {
        execOps.exec {
            workingDir = rustProjectDir.get().asFile
            commandLine(cargoBin.get(), "build", "--release")
        }
    }
}
