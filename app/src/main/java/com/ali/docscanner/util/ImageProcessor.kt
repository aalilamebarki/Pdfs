package com.ali.docscanner.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {

    /**
     * Decodes a bitmap from disk downsampled to roughly [reqWidth]x[reqHeight] to avoid
     * loading a full-resolution camera image into memory unnecessarily, then corrects
     * orientation using the file's EXIF tag (android.media.ExifInterface — already
     * available on API 26+, no extra dependency needed).
     */
    fun decodeSampledBitmap(file: File, reqWidth: Int = 1600, reqHeight: Int = 1600): Bitmap {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        boundsOptions.inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
        boundsOptions.inJustDecodeBounds = false

        val bitmap = BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            ?: error("Unable to decode image at ${file.absolutePath}")

        return correctOrientation(bitmap, file)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun correctOrientation(bitmap: Bitmap, file: File): Bitmap {
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Best-effort, NATIVE-ONLY heuristic for locating a document's edges within [bitmap].
     *
     * IMPORTANT — this is intentionally NOT professional-grade contour/edge detection.
     * A real implementation (perspective-aware quadrilateral detection) would require
     * OpenCV, which per project rules is not added without explicit approval. This
     * heuristic downscales the image, converts to grayscale, and scans inward from each
     * of the four sides for the first row/column where local brightness contrast exceeds
     * a threshold — approximating where a page edge meets a contrasting background (e.g.
     * a desk). It works reasonably for a page centered on a plain, contrasting surface,
     * and works poorly on cluttered or low-contrast backgrounds.
     *
     * Returns a normalized RectF (0f..1f) as a starting guide only. Interactive
     * "magnetic corner" adjustment is Phase 3 scope. Its accuracy will be evaluated in
     * Phase 3, at which point a decision on whether OpenCV is actually needed will be
     * made explicitly with the project owner.
     */
    fun detectDocumentBounds(bitmap: Bitmap): RectF {
        val defaultBounds = RectF(0.06f, 0.08f, 0.94f, 0.92f)

        return try {
            val sampleSize = 200
            val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, true)
            val pixels = IntArray(sampleSize * sampleSize)
            scaled.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)

            val gray = IntArray(sampleSize * sampleSize)
            for (i in pixels.indices) {
                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                gray[i] = (r * 299 + g * 587 + b * 114) / 1000
            }

            val threshold = 18
            val minMargin = (sampleSize * 0.04f).toInt()
            val maxMarginFromEdge = (sampleSize * 0.35f).toInt()

            val left = scanEdge(gray, sampleSize, minMargin, maxMarginFromEdge, threshold, Edge.LEFT)
            val right = scanEdge(gray, sampleSize, minMargin, maxMarginFromEdge, threshold, Edge.RIGHT)
            val top = scanEdge(gray, sampleSize, minMargin, maxMarginFromEdge, threshold, Edge.TOP)
            val bottom = scanEdge(gray, sampleSize, minMargin, maxMarginFromEdge, threshold, Edge.BOTTOM)

            if (left == null || right == null || top == null || bottom == null || left >= right || top >= bottom) {
                defaultBounds
            } else {
                RectF(
                    left / sampleSize.toFloat(),
                    top / sampleSize.toFloat(),
                    right / sampleSize.toFloat(),
                    bottom / sampleSize.toFloat()
                )
            }
        } catch (e: Exception) {
            defaultBounds
        }
    }

    private enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

    private fun scanEdge(
        gray: IntArray,
        size: Int,
        minMargin: Int,
        maxMargin: Int,
        threshold: Int,
        edge: Edge
    ): Int? {
        for (offset in minMargin until maxMargin) {
            var diffSum = 0
            var samples = 0
            when (edge) {
                Edge.LEFT -> for (y in 0 until size step 4) {
                    val idx = y * size + offset
                    val idxPrev = y * size + max(0, offset - 2)
                    diffSum += abs(gray[idx] - gray[idxPrev])
                    samples++
                }
                Edge.RIGHT -> for (y in 0 until size step 4) {
                    val x = size - 1 - offset
                    val idx = y * size + x
                    val idxNext = y * size + min(size - 1, x + 2)
                    diffSum += abs(gray[idx] - gray[idxNext])
                    samples++
                }
                Edge.TOP -> for (x in 0 until size step 4) {
                    val idx = offset * size + x
                    val idxPrev = max(0, offset - 2) * size + x
                    diffSum += abs(gray[idx] - gray[idxPrev])
                    samples++
                }
                Edge.BOTTOM -> for (x in 0 until size step 4) {
                    val y = size - 1 - offset
                    val idx = y * size + x
                    val idxNext = min(size - 1, y + 2) * size + x
                    diffSum += abs(gray[idx] - gray[idxNext])
                    samples++
                }
            }
            val avgDiff = if (samples > 0) diffSum / samples else 0
            if (avgDiff > threshold) {
                return when (edge) {
                    Edge.LEFT -> offset
                    Edge.RIGHT -> size - 1 - offset
                    Edge.TOP -> offset
                    Edge.BOTTOM -> size - 1 - offset
                }
            }
        }
        return null
    }
}
