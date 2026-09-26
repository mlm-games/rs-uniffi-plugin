package org.mlm.rustuniffi

import org.gradle.internal.os.OperatingSystem
import java.io.File

object PlatformUtil {

    val os: OperatingSystem = OperatingSystem.current()

    fun hostLibName(crateName: String): String = when {
        os.isMacOsX  -> "lib${crateName}.dylib"
        os.isWindows -> "${crateName}.dll"
        else         -> "lib${crateName}.so"
    }

    /**
     * Name of the cdylib built for Android. Independent of the build host: android targets are
     * ELF, so cargo ndk always emits `lib<name>.so` even when building from macOS or Windows.
     */
    fun androidLibName(crateName: String): String = "lib${crateName}.so"

    private val SECTION = Regex("""^\[([A-Za-z0-9_.-]+)]$""")
    private val NAME = Regex("""^name\s*=\s*"([^"]+)"""")

    /**
     * The cdylib name cargo would use for [cargoToml]: `[lib] name` when present, otherwise
     * `[package] name`. Scans the whole file so either section may appear first, and skips other
     * sections such as `[[bin]]` so their names are not mistaken for the crate name.
     */
    fun crateNameFromCargoToml(cargoToml: File): String? {
        if (!cargoToml.isFile) return null
        var packageName: String? = null
        var libName: String? = null
        var section: String? = null
        cargoToml.forEachLine { raw ->
            val line = raw.substringBefore('#').trim()
            SECTION.matchEntire(line)?.let { section = it.groupValues[1] }
            if (section != "package" && section != "lib") return@forEachLine
            val name = NAME.matchEntire(line)?.groupValues?.get(1) ?: return@forEachLine
            if (section == "lib") libName = name else packageName = name
        }
        return libName ?: packageName
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
