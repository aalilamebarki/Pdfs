package com.ali.docscanner.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

enum class PdfQuality(val jpegQuality: Int, val maxDimension: Int) {
    LOW(50, 1000),
    MEDIUM(75, 1600),
    HIGH(92, 2400)
}

object PdfGenerator {

    /**
     * Builds a multi-page PDF where each page is sized to match its source image's aspect
     * ratio (scaled to a standard width), embedding a JPEG-compressed copy of each page
     * image at the requested [quality]. Uses android.graphics.pdf.PdfDocument — a native
     * Android API, no external PDF library required.
     */
    fun generate(imagePaths: List<String>, outputFile: File, quality: PdfQuality): File {
        require(imagePaths.isNotEmpty()) { "Cannot generate a PDF with zero pages" }

        val document = PdfDocument()

        try {
            imagePaths.forEach { path ->
                val bitmap = decodeForPdf(path, quality)
                val pageWidthPt = 595 // A4 width in points at 72dpi
                val pageHeightPt = (pageWidthPt * bitmap.height.toFloat() / bitmap.width).toInt()

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, document.pages.size + 1).create()
                val page = document.startPage(pageInfo)

                val destRect = Rect(0, 0, pageWidthPt, pageHeightPt)
                page.canvas.drawBitmap(bitmap, null, destRect, null)

                document.finishPage(page)
                bitmap.recycle()
            }

            FileOutputStream(outputFile).use { document.writeTo(it) }
        } finally {
            document.close()
        }

        return outputFile
    }

    private fun decodeForPdf(path: String, quality: PdfQuality): Bitmap {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)

        var sampleSize = 1
        val largestSide = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
        while (largestSide / sampleSize > quality.maxDimension * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var decoded = BitmapFactory.decodeFile(path, decodeOptions)
            ?: error("Unable to decode page image at $path")

        val currentLargest = maxOf(decoded.width, decoded.height)
        if (currentLargest > quality.maxDimension) {
            val scale = quality.maxDimension.toFloat() / currentLargest
            val scaled = Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled !== decoded) decoded.recycle()
            decoded = scaled
        }

        // Re-encode through JPEG at the requested quality so "Low/Medium/High" actually
        // affects output file size, not just resolution (android.graphics.pdf.PdfDocument
        // has no public API to set compression quality on drawn bitmaps directly).
        val compressedBytes = ByteArrayOutputStream().use { stream ->
            decoded.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, stream)
            stream.toByteArray()
        }
        val recompressed = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
        if (recompressed !== decoded) decoded.recycle()

        return recompressed ?: error("Unable to re-encode page image at $path")
    }
}
