package com.farsh.carpetmapreader.processor

import android.graphics.Bitmap
import android.graphics.Color
import com.farsh.carpetmapreader.data.MapCell
import kotlin.math.pow
import kotlin.math.sqrt

class ImageGridDetector {

    // A curated map of traditional Persian carpet yarn colors with standard RGB templates
    private val carpetColors = listOf(
        CarpetColorTemplate("قرمز", 214, 40, 40, "#D62828"),
        CarpetColorTemplate("آبی", 30, 144, 255, "#1E90FF"),
        CarpetColorTemplate("سبز", 40, 116, 101, "#287465"),
        CarpetColorTemplate("زرد", 252, 191, 73, "#FCBF49"),
        CarpetColorTemplate("نارنجی", 247, 127, 0, "#F77F00"),
        CarpetColorTemplate("سورمه‌ای", 10, 25, 47, "#0A192F"),
        CarpetColorTemplate("فیروزه‌ای", 72, 202, 228, "#48CAE4"),
        CarpetColorTemplate("کرم", 242, 234, 211, "#F2EAD3"),
        CarpetColorTemplate("صورتی", 255, 175, 204, "#FFAFCC"),
        CarpetColorTemplate("قهوه‌ای", 110, 68, 30, "#6E441E"),
        CarpetColorTemplate("سفید", 245, 245, 245, "#F5F5F5"),
        CarpetColorTemplate("مشکی", 25, 25, 25, "#191919"),
        CarpetColorTemplate("خاکستری", 128, 128, 128, "#808080"),
        CarpetColorTemplate("بژ", 225, 198, 153, "#E1C699")
    )

    data class CarpetColorTemplate(
        val name: String,
        val r: Int,
        val g: Int,
        val b: Int,
        val hex: String
    )

    /**
     * Splits the input bitmap into grid segments and processes each segment
     * to detect dominant color and extract a clean sub-bitmap for OCR.
     */
    fun detectGrid(
        bitmap: Bitmap,
        rows: Int,
        cols: Int,
        projectId: Long
    ): List<ProcessedCell> {
        val width = bitmap.width
        val height = bitmap.height

        val cellWidth = width / cols
        val cellHeight = height / rows

        val cells = mutableListOf<ProcessedCell>()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                // Calculate bounding box
                val startX = c * cellWidth
                val startY = r * cellHeight
                
                // Adjust for edge boundaries
                val currentWidth = if (startX + cellWidth <= width) cellWidth else (width - startX)
                val currentHeight = if (startY + cellHeight <= height) cellHeight else (height - startY)

                if (currentWidth <= 0 || currentHeight <= 0) continue

                // 1. Crop the sub-bitmap for this cell
                val cellBitmap = Bitmap.createBitmap(bitmap, startX, startY, currentWidth, currentHeight)

                // 2. Extract dominant color from the inner 60% of the cell, avoiding grid lines
                val rgb = extractDominantColor(cellBitmap)
                val colorTemplate = findNearestCarpetColor(rgb.first, rgb.second, rgb.third)

                // 3. Create high contrast cell bitmap for better OCR
                val highContrastBitmap = enhanceContrast(cellBitmap)

                val mapCell = MapCell(
                    projectId = projectId,
                    rowIdx = r,
                    colIdx = c,
                    number = null, // To be updated by OCR on succeeding steps
                    colorHex = colorTemplate.hex,
                    colorName = colorTemplate.name,
                    isRead = false
                )

                cells.add(ProcessedCell(mapCell, highContrastBitmap))
            }
        }

        return cells
    }

    /**
     * Extracts dominant RGB values from the central region of the cell
     * to eliminate black/white/colored structural grids surrounding cells.
     */
    private fun extractDominantColor(cellBitmap: Bitmap): Triple<Int, Int, Int> {
        val w = cellBitmap.width
        val h = cellBitmap.height

        // Define core sampling window (inner 60% to avoid boundaries)
        val startX = (w * 0.20).toInt().coerceAtLeast(0)
        val startY = (h * 0.20).toInt().coerceAtLeast(0)
        val endX = (w * 0.80).toInt().coerceAtMost(w - 1)
        val endY = (h * 0.80).toInt().coerceAtMost(h - 1)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var sampleCount = 0

        // Adaptive spatial sampling step to speed up computation
        val stepX = ((endX - startX) / 10).coerceAtLeast(1)
        val stepY = ((endY - startY) / 10).coerceAtLeast(1)

        for (x in startX..endX step stepX) {
            for (y in startY..endY step stepY) {
                if (x >= w || y >= h) continue
                val pixel = cellBitmap.getPixel(x, y)
                totalR += Color.red(pixel)
                totalG += Color.green(pixel)
                totalB += Color.blue(pixel)
                sampleCount++
            }
        }

        if (sampleCount == 0) return Triple(128, 128, 128)

        return Triple(
            (totalR / sampleCount).toInt(),
            (totalG / sampleCount).toInt(),
            (totalB / sampleCount).toInt()
        )
    }

    /**
     * Match real RGB reading against traditional Iranian carpet colors
     * using Euclidean distance in 3D color space.
     */
    fun findNearestCarpetColor(r: Int, g: Int, b: Int): CarpetColorTemplate {
        var minDistance = Double.MAX_VALUE
        var nearest = carpetColors[10] // Default to White / Cream in case

        for (template in carpetColors) {
            val dist = sqrt(
                (template.r - r).toDouble().pow(2.0) +
                (template.g - g).toDouble().pow(2.0) +
                (template.b - b).toDouble().pow(2.0)
            )
            if (dist < minDistance) {
                minDistance = dist
                nearest = template
            }
        }
        return nearest
    }

    /**
     * Pure-Kotlin contrast & threshold enhancer. Multiplying values from middle gray
     * increases difference between numbers (dark pen) and carpet backgrounds (bright grid colors).
     */
    fun enhanceContrast(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)

        val contrastFactor = 1.4f // Increase contrast by 40%

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = src.getPixel(x, y)
                val alpha = Color.alpha(pixel)

                // Apply simple contrast stretch on individual channels
                var r = (((Color.red(pixel) - 128) * contrastFactor) + 128).toInt().coerceIn(0, 255)
                var g = (((Color.green(pixel) - 128) * contrastFactor) + 128).toInt().coerceIn(0, 255)
                var b = (((Color.blue(pixel) - 128) * contrastFactor) + 128).toInt().coerceIn(0, 255)

                dest.setPixel(x, y, Color.argb(alpha, r, g, b))
            }
        }
        return dest
    }

    data class ProcessedCell(
        val cell: MapCell,
        val cellBitmap: Bitmap
    )
}
