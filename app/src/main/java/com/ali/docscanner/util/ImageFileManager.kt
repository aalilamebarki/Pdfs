package com.ali.docscanner.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Local file-storage strategy for scanned pages and exported PDFs.
 *
 * - cacheDir/scans/   -> temporary captures/crops (camera output, gallery import copies,
 *                        intermediate crop results). Safe to purge at any time; explicitly
 *                        cleared when the user exits the scan flow without confirming.
 * - filesDir/pages/   -> permanent, user-confirmed page images + their thumbnails.
 * - filesDir/exports/ -> generated multi-page PDF exports, one file per document
 *                        (regenerated on demand rather than cached indefinitely).
 */
object ImageFileManager {

    private const val TEMP_DIR_NAME = "scans"
    private const val PERMANENT_DIR_NAME = "pages"
    private const val EXPORTS_DIR_NAME = "exports"

    private const val THUMBNAIL_MAX_DIMENSION = 320
    private const val THUMBNAIL_QUALITY = 80

    fun createTempCaptureFile(context: Context): File = createTempFile(context, "capture")

    fun createTempFile(context: Context, prefix: String): File {
        val dir = File(context.cacheDir, TEMP_DIR_NAME).apply { mkdirs() }
        val unique = UUID.randomUUID().toString().take(8)
        return File(dir, "${prefix}_${System.currentTimeMillis()}_$unique.jpg")
    }

    fun pagesDir(context: Context): File = File(context.filesDir, PERMANENT_DIR_NAME).apply { mkdirs() }

    fun exportsDir(context: Context): File = File(context.filesDir, EXPORTS_DIR_NAME).apply { mkdirs() }

    fun moveToPermanentStorage(context: Context, tempFile: File): File {
        val destination = File(pagesDir(context), "page_${UUID.randomUUID()}.jpg")
        tempFile.copyTo(destination, overwrite = true)
        tempFile.delete()
        return destination
    }

    /**
     * Writes a processed (cropped + filtered) bitmap directly to permanent storage along
     * with a small thumbnail, returning both paths. Used at the end of the Crop -> Filter
     * pipeline (Phase 3/4), where the final image only exists in memory.
     */
    fun savePermanentBitmap(context: Context, bitmap: Bitmap, quality: Int): PermanentImageResult {
        val id = UUID.randomUUID().toString()
        val imageFile = File(pagesDir(context), "page_$id.jpg")
        FileOutputStream(imageFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }

        val thumbFile = File(pagesDir(context), "thumb_$id.jpg")
        val thumbBitmap = downscale(bitmap, THUMBNAIL_MAX_DIMENSION)
        FileOutputStream(thumbFile).use {
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, it)
        }
        if (thumbBitmap !== bitmap) thumbBitmap.recycle()

        return PermanentImageResult(imageFile.absolutePath, thumbFile.absolutePath)
    }

    private fun downscale(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun deleteFile(file: File?) {
        if (file != null && file.exists()) {
            file.delete()
        }
    }

    fun deleteFileAtPath(path: String?) {
        if (path.isNullOrBlank()) return
        deleteFile(File(path))
    }

    fun exportedPdfFile(context: Context, documentId: Long): File {
        return File(exportsDir(context), "document_$documentId.pdf")
    }

    fun deleteExportedPdfFor(context: Context, documentId: Long) {
        deleteFile(exportedPdfFile(context, documentId))
    }

    /** Clears any leftover temporary captures — called when the user cancels the scan flow. */
    fun clearTempCache(context: Context) {
        val dir = File(context.cacheDir, TEMP_DIR_NAME)
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }
}

data class PermanentImageResult(val imagePath: String, val thumbnailPath: String)

