package com.ali.docscanner.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.hypot

/**
 * Performs perspective (projective) correction using only native Android APIs.
 *
 * [android.graphics.Matrix.setPolyToPoly] with 4 point pairs computes a full projective
 * transform mapping one quadrilateral onto another — this is exactly what's needed to
 * "flatten" a photographed document into a rectangle, without requiring OpenCV.
 */
object PerspectiveTransformer {

    /**
     * @param corners 8 floats: [x0,y0, x1,y1, x2,y2, x3,y3] in TL, TR, BR, BL order,
     *                in the source bitmap's pixel coordinate space.
     */
    fun warp(bitmap: Bitmap, corners: FloatArray): Bitmap {
        require(corners.size == 8) { "corners must contain exactly 4 points (8 floats)" }

        val topWidth = distance(corners[0], corners[1], corners[2], corners[3])
        val bottomWidth = distance(corners[6], corners[7], corners[4], corners[5])
        val leftHeight = distance(corners[0], corners[1], corners[6], corners[7])
        val rightHeight = distance(corners[2], corners[3], corners[4], corners[5])

        val dstWidth = maxOf(topWidth, bottomWidth).toInt().coerceAtLeast(1)
        val dstHeight = maxOf(leftHeight, rightHeight).toInt().coerceAtLeast(1)

        val dst = floatArrayOf(
            0f, 0f,
            dstWidth.toFloat(), 0f,
            dstWidth.toFloat(), dstHeight.toFloat(),
            0f, dstHeight.toFloat()
        )

        val matrix = Matrix()
        val mapped = matrix.setPolyToPoly(corners, 0, dst, 0, 4)
        if (!mapped) return bitmap

        val output = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
        return output
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
}
