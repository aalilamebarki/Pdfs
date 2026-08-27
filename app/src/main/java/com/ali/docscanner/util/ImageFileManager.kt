package com.ali.docscanner.util

import android.content.Context
import java.io.File
import java.util.UUID

/**
 * Local file-storage strategy for scanned pages.
 *
 * - cacheDir/scans/  -> temporary captures (camera output or gallery import copies).
 *                       Safe to purge at any time; explicitly cleared when the user
 *                       exits the scan flow without confirming/saving a page.
 * - filesDir/pages/  -> permanent, user-confirmed page images.
 *
 * NOTE: Linking a saved page file to a specific Document/Page Room entity (multi-page
 * documents, reordering, deletion) is Phase 4 scope. Phase 2 only proves the file
 * pipeline: capture/import -> temp -> user confirms -> permanent storage.
 */
object ImageFileManager {

    private const val TEMP_DIR_NAME = "scans"
    private const val PERMANENT_DIR_NAME = "pages"

    fun createTempCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, TEMP_DIR_NAME).apply { mkdirs() }
        return File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }

    fun moveToPermanentStorage(context: Context, tempFile: File): File {
        val dir = File(context.filesDir, PERMANENT_DIR_NAME).apply { mkdirs() }
        val destination = File(dir, "page_${UUID.randomUUID()}.jpg")
        tempFile.copyTo(destination, overwrite = true)
        tempFile.delete()
        return destination
    }

    fun deleteFile(file: File?) {
        if (file != null && file.exists()) {
            file.delete()
        }
    }

    /** Clears any leftover temporary captures — called when the user cancels the scan flow. */
    fun clearTempCache(context: Context) {
        val dir = File(context.cacheDir, TEMP_DIR_NAME)
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }
}
