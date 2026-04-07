package org.krripe.scobbleboosters.utils

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object FileIO {
    fun safeReadText(file: File): String? = runCatching {
        if (!file.exists()) null else file.readText(StandardCharsets.UTF_8)
    }.getOrNull()

    fun atomicWriteText(file: File, text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text, StandardCharsets.UTF_8)

        runCatching {
            Files.move(
                tmp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.getOrElse {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }
}
