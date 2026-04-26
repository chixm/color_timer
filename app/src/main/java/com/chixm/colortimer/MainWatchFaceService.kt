package com.chixm.colortimer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Watch face that shows time, date, and heart rate.
 */
class MainWatchFaceService : WatchFaceService() {

    override fun createUserStyleSchema(): UserStyleSchema = UserStyleSchema(emptyList())

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = MainWatchFaceRenderer(
            applicationContext,
            surfaceHolder,
            watchState,
            currentUserStyleRepository,
            CanvasType.SOFTWARE
        )

        return WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        )
    }
}

private class MainWatchFaceRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<Renderer.SharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    interactiveDrawModeUpdateDelayMillis = 16L,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    override suspend fun createSharedAssets(): Renderer.SharedAssets =
        object : Renderer.SharedAssets {
            override fun onDestroy() {}
        }

    override suspend fun init() {
        // Initialize any watch face-specific resources here if needed
    }

    private val timePaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val datePaint = Paint().apply {
        color = Color.GRAY
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val heartRatePaint = Paint().apply {
        color = Color.RED
        textSize = 32f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val backgroundPaint = Paint().apply {
        color = Color.BLUE
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: Renderer.SharedAssets
    ) {
        try {
            canvas.drawRect(bounds, backgroundPaint)

            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()

            val timeText = zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            canvas.drawText(
                timeText,
                centerX,
                centerY - 50,
                timePaint
            )

            val dateText = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd (EEE)", Locale.getDefault()))
            canvas.drawText(
                dateText,
                centerX,
                centerY - 20,
                datePaint
            )

            val heartRate = getHeartRate()
            val heartRateText = "HR: $heartRate"
            canvas.drawText(
                heartRateText,
                centerX,
                centerY + 20,
                heartRatePaint
            )
        } catch (e: Exception) {
            // Prevent renderer crashes from bringing down the watch face
            e.printStackTrace()
        }
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: Renderer.SharedAssets
    ) {
        canvas.drawColor(Color.argb(128, 255, 255, 255))
    }

    private fun getHeartRate(): String {
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS)
            != PackageManager.PERMISSION_GRANTED) {
            "No Perm"
        } else {
            val sharedPreferences = context.getSharedPreferences("heart_rate_prefs", Context.MODE_PRIVATE)
            val heartRate = sharedPreferences.getInt("latest_heart_rate", 0)
            if (heartRate > 0) heartRate.toString() else "No Data"
        }
    }
}