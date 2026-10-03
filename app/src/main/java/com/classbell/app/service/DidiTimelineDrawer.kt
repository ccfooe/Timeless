package com.classbell.app.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

object DidiTimelineDrawer {

    /**
     * Draws a Didi-style milestone timeline onto a Bitmap.
     * Includes background rail, theme-colored progress line, 3 milestone nodes,
     * floating runner pill with dynamic themed bell icon, and clean text labels.
     */
    fun drawTimelineBitmap(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        progress: Float,
        accentColor: Int,
        startLabel: String,
        startTime: String,
        midLabel: String,
        midTime: String,
        endLabel: String,
        endTime: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(100), heightPx.coerceAtLeast(60), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = context.resources.displayMetrics.density

        val paddingHorizontal = 42f * density
        val startX = paddingHorizontal
        val endX = (widthPx - paddingHorizontal).coerceAtLeast(startX + 60f * density)
        val totalTrackWidth = endX - startX
        val midX = (startX + endX) / 2f
        val trackY = 17f * density

        val clampedProgress = progress.coerceIn(0f, 1f)
        val currentX = startX + clampedProgress * totalTrackWidth

        // 1. Base Inactive Track
        val trackThickness = 3.5f * density
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30323E")
            style = Paint.Style.FILL
        }
        val trackRect = RectF(startX, trackY - trackThickness / 2f, endX, trackY + trackThickness / 2f)
        canvas.drawRoundRect(trackRect, trackThickness / 2f, trackThickness / 2f, trackPaint)

        // 2. Active Filled Track
        if (clampedProgress > 0.005f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = trackThickness
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(startX, trackY, currentX, trackY, progressPaint)
        }

        // 3. Milestone Nodes
        val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Node 1 (Start): Distinctive Outer Ring + Center Dot in Accent Color (matches Didi start node)
        nodePaint.style = Paint.Style.STROKE
        nodePaint.strokeWidth = 2.2f * density
        nodePaint.color = accentColor
        canvas.drawCircle(startX, trackY, 5.5f * density, nodePaint)

        nodePaint.style = Paint.Style.FILL
        nodePaint.color = Color.parseColor("#1B1C23")
        canvas.drawCircle(startX, trackY, 4.4f * density, nodePaint)

        nodePaint.color = accentColor
        canvas.drawCircle(startX, trackY, 2f * density, nodePaint)

        // Node 2 (Midpoint): Circle dot
        val isMidPassed = clampedProgress >= 0.5f
        if (isMidPassed) {
            nodePaint.style = Paint.Style.FILL
            nodePaint.color = accentColor
            canvas.drawCircle(midX, trackY, 4.5f * density, nodePaint)

            nodePaint.color = Color.WHITE
            canvas.drawCircle(midX, trackY, 2f * density, nodePaint)
        } else {
            nodePaint.style = Paint.Style.FILL
            nodePaint.color = Color.parseColor("#424455")
            canvas.drawCircle(midX, trackY, 4f * density, nodePaint)

            nodePaint.color = Color.parseColor("#262734")
            canvas.drawCircle(midX, trackY, 1.8f * density, nodePaint)
        }

        // Node 3 (End): Circle dot
        val isEndPassed = clampedProgress >= 0.99f
        if (isEndPassed) {
            nodePaint.style = Paint.Style.FILL
            nodePaint.color = accentColor
            canvas.drawCircle(endX, trackY, 4.5f * density, nodePaint)

            nodePaint.color = Color.WHITE
            canvas.drawCircle(endX, trackY, 2f * density, nodePaint)
        } else {
            nodePaint.style = Paint.Style.FILL
            nodePaint.color = Color.parseColor("#424455")
            canvas.drawCircle(endX, trackY, 4f * density, nodePaint)

            nodePaint.color = Color.parseColor("#262734")
            canvas.drawCircle(endX, trackY, 1.8f * density, nodePaint)
        }

        // 4. Moving Runner Pill (Didi-style vehicle / indicator)
        val runnerCenterX = currentX.coerceIn(startX, endX)
        val pillWidth = 26f * density
        val pillHeight = 16f * density
        val pillCorner = 8f * density

        // Runner drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(
            runnerCenterX - pillWidth / 2f,
            trackY - pillHeight / 2f + 1f * density,
            runnerCenterX + pillWidth / 2f,
            trackY + pillHeight / 2f + 1f * density
        )
        canvas.drawRoundRect(shadowRect, pillCorner, pillCorner, shadowPaint)

        // Runner pill body (pure white badge)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val pillRect = RectF(
            runnerCenterX - pillWidth / 2f,
            trackY - pillHeight / 2f,
            runnerCenterX + pillWidth / 2f,
            trackY + pillHeight / 2f
        )
        canvas.drawRoundRect(pillRect, pillCorner, pillCorner, pillPaint)

        // Crisp Bell Icon inside the runner pill (tinted with accent color)
        drawBellIcon(canvas, runnerCenterX, trackY, 9f * density, accentColor)

        // 5. Milestone Text Labels (under the nodes)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 10.5f * density
            color = Color.parseColor("#9DA0AE")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 9.5f * density
            color = Color.parseColor("#6F7284")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val labelY = trackY + 15f * density
        val timeY = labelY + 11f * density

        // Start Label & Time
        canvas.drawText(startLabel, startX, labelY, labelPaint)
        if (startTime.isNotBlank()) {
            canvas.drawText(startTime, startX, timeY, timePaint)
        }

        // Mid Label & Time
        canvas.drawText(midLabel, midX, labelY, labelPaint)
        if (midTime.isNotBlank()) {
            canvas.drawText(midTime, midX, timeY, timePaint)
        }

        // End Label & Time
        canvas.drawText(endLabel, endX, labelY, labelPaint)
        if (endTime.isNotBlank()) {
            canvas.drawText(endTime, endX, timeY, timePaint)
        }

        return bitmap
    }

    /**
     * Draws a crisp vector-style school bell icon centered at (cx, cy).
     */
    private fun drawBellIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        val half = size / 2f
        val top = cy - half
        val bottom = cy + half * 0.7f

        // Top loop / crown
        val crownRadius = size * 0.12f
        canvas.drawCircle(cx, top + crownRadius, crownRadius, paint)

        // Bell Body Path
        val path = Path()
        val neckTop = top + crownRadius * 1.5f
        val neckHalfWidth = size * 0.2f
        val baseHalfWidth = size * 0.44f

        path.moveTo(cx - neckHalfWidth, neckTop)
        // Left side flare
        path.quadTo(cx - neckHalfWidth * 1.1f, cy, cx - baseHalfWidth, bottom)
        // Bottom rim
        path.lineTo(cx + baseHalfWidth, bottom)
        // Right side flare
        path.quadTo(cx + neckHalfWidth * 1.1f, cy, cx + neckHalfWidth, neckTop)
        path.close()
        canvas.drawPath(path, paint)

        // Bottom clapper dot
        val clapperRadius = size * 0.1f
        canvas.drawCircle(cx, bottom + clapperRadius * 0.8f, clapperRadius, paint)
    }
}
