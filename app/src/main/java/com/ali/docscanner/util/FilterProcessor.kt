package com.ali.docscanner.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

enum class DocumentFilter {
    ORIGINAL,
    GRAYSCALE,
    BLACK_AND_WHITE,
    ENHANCED
}

object FilterProcessor {

    fun apply(source: Bitmap, filter: DocumentFilter): Bitmap {
        return when (filter) {
            DocumentFilter.ORIGINAL -> source
            DocumentFilter.GRAYSCALE -> applyColorMatrix(source, grayscaleMatrix())
            DocumentFilter.BLACK_AND_WHITE -> applyThreshold(source)
            DocumentFilter.ENHANCED -> applyColorMatrix(source, enhancedMatrix())
        }
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private fun grayscaleMatrix(): ColorMatrix = ColorMatrix().apply { setSaturation(0f) }

    /** Mild contrast + saturation boost to approximate a "Magic Color / Enhanced" look. */
    private fun enhancedMatrix(): ColorMatrix {
        val contrast = 1.15f
        val brightnessOffset = -12f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightnessOffset,
                0f, contrast, 0f, 0f, brightnessOffset,
                0f, 0f, contrast, 0f, brightnessOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val saturationMatrix = ColorMatrix().apply { setSaturation(1.25f) }
        contrastMatrix.postConcat(saturationMatrix)
        return contrastMatrix
    }

    /**
     * True black & white via per-pixel luminance thresholding (not just desaturation).
     * Downscales work implicitly by operating on the already-cropped page bitmap.
     */
    private fun applyThreshold(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val threshold = computeAdaptiveThreshold(pixels)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val luminance = (r * 299 + g * 587 + b * 114) / 1000
            pixels[i] = if (luminance >= threshold) {
                0xFFFFFFFF.toInt()
            } else {
                0xFF000000.toInt()
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    /** Simple mean-based threshold — adequate for a document scanner MVP, not a full Otsu implementation. */
    private fun computeAdaptiveThreshold(pixels: IntArray): Int {
        var sum = 0L
        val step = maxOf(1, pixels.size / 10000) // sample for performance on large images
        var sampleCount = 0
        var i = 0
        while (i < pixels.size) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            sum += (r * 299 + g * 587 + b * 114) / 1000
            sampleCount++
            i += step
        }
        return if (sampleCount > 0) (sum / sampleCount).toInt() else 128
    }
}
