package com.example.voicedrop.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipManager {

    fun compress(sourceFile: File, destDir: File): File {
        destDir.mkdirs()
        val zipFile = File(destDir, "${sourceFile.nameWithoutExtension}.zip")

        val crc = calculateCrc(sourceFile)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            val entry = ZipEntry(sourceFile.name).apply {
                method = ZipEntry.STORED
                size = sourceFile.length()
                compressedSize = sourceFile.length()
                setCrc(crc)
            }
            zos.putNextEntry(entry)

            BufferedInputStream(FileInputStream(sourceFile)).use { bis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (bis.read(buffer).also { bytesRead = it } != -1) {
                    zos.write(buffer, 0, bytesRead)
                }
            }

            zos.closeEntry()
        }

        return zipFile
    }

    private fun calculateCrc(file: File): Long {
        val crc32 = CRC32()
        BufferedInputStream(FileInputStream(file)).use { bis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (bis.read(buffer).also { bytesRead = it } != -1) {
                crc32.update(buffer, 0, bytesRead)
            }
        }
        return crc32.value
    }
}
