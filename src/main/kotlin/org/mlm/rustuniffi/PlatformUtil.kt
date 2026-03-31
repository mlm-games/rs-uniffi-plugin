package org.mlm.rustuniffi

import org.gradle.internal.os.OperatingSystem

object PlatformUtil {

    val os: OperatingSystem = OperatingSystem.current()

    fun hostLibName(crateName: String): String = when {
        os.isMacOsX  -> "lib${crateName}.dylib"
        os.isWindows -> "${crateName}.dll"
        else         -> "lib${crateName}.so"
    }

    val cargoBin: String get() = if (os.isWindows) "cargo.exe" else "cargo"

    val jnaPlatformDir: String by lazy {
        val arch = System.getProperty("os.arch").lowercase()
        when {
            os.isLinux   && arch.isArm64() -> "linux-aarch64"
            os.isLinux                     -> "linux-x86-64"
            os.isMacOsX  && arch.isArm64() -> "darwin-aarch64"
            os.isMacOsX                    -> "darwin"
            os.isWindows && arch.contains("64") -> "win32-x86-64"
            os.isWindows                   -> "win32-x86"
            else -> error("Unsupported OS/arch: ${System.getProperty("os.name")} $arch")
        }
    }

    private fun String.isArm64() = contains("aarch64") || contains("arm64")
}
