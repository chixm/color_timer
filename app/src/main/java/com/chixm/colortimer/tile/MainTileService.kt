package com.chixm.colortimer.tile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.chixm.colortimer.HeartRateService
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.chixm.colortimer.R
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.tools.LayoutRootPreview
import com.google.android.horologist.compose.tools.buildDeviceParameters
import com.google.android.horologist.tiles.SuspendingTileService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val RESOURCES_VERSION = "0"

/**
 * Tile that shows time and heart rate on a light blue background.
 */
private const val TAG = "MainTileService"

@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ResourceBuilders.Resources {
        // 画像リソースを登録（API 34+）
        val builder = ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION)
        builder.addIdToImageMapping(
            "bg_image",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.tile_preview)
                        .build()
                )
                .build()
        )
        // tile_preview2.png を bg_image2 として登録
        builder.addIdToImageMapping(
            "bg_image2",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.tile_preview2)
                        .build()
                )
                .build()
        )
        return builder.build()
    }

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        applicationContext.startService(Intent(applicationContext, HeartRateService::class.java))

        val heartRate = try {
            getHeartRate()
        } catch (e: Exception) {
            Log.w(TAG, "tileRequest failed", e)
            "No Data"
        }
        val heartRateInt = heartRate.toIntOrNull() ?: 0

        val timelineBuilder = TimelineBuilders.Timeline.Builder()

        if (heartRateInt > 100) {
            val now = Date()
            val sec = SimpleDateFormat("ss", Locale.getDefault()).format(now).toIntOrNull() ?: 0
            val isRed = sec % 2 == 1
            val color = if (isRed) 0xFFFF0000.toInt() else 0xFF000000.toInt() // 赤 or 黒
            timelineBuilder.addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(tileLayout(this, heartRate, color)).build()
                    )
                    .build()
            )
        } else {
            timelineBuilder.addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(tileLayout(this, heartRate, -1)).build() // -1: gradient
                    )
                    .build()
            )
        }

        return TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timelineBuilder.build()).build()
    }

    private fun getHeartRate(): String {
        return if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.BODY_SENSORS) 
            != PackageManager.PERMISSION_GRANTED) {
            "No Perm"
        } else {
            val sharedPreferences = applicationContext.getSharedPreferences("heart_rate_prefs", MODE_PRIVATE)
            val heartRate = sharedPreferences.getInt("latest_heart_rate", 0)
            if (heartRate > 0) heartRate.toString() else "No Data"
        }
    }
}

private fun tileLayout(context: Context, heartRate: String, backgroundColor: Int): LayoutElementBuilders.LayoutElement {
    val now = Date()
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(now)
    val weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(now)

    // API 34以降: 画像背景を利用
    if (backgroundColor == -1) {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            // 画像を背景に配置
            .addContent(
                LayoutElementBuilders.Image.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .setResourceId("bg_image")
                    .build()
            )
            .addContent(
                PrimaryLayout.Builder(buildDeviceParameters(context.resources))
                    .setResponsiveContentInsetEnabled(true)
                    .setContent(
                        LayoutElementBuilders.Column.Builder()
                            .addContent(
                                Text.Builder(context, time)
                                    .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                                    .setColor(ColorBuilders.argb(0xFF000000.toInt()))
                                    .build()
                            )
                            .addContent(
                                LayoutElementBuilders.Spacer.Builder()
                                    .setHeight(DimensionBuilders.dp(0f))
                                    .build()
                            )
                            .addContent(
                                Text.Builder(context, "$date ($weekday)")
                                    .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                    .setColor(ColorBuilders.argb(0xFF333333.toInt()))
                                    .build()
                            )
                            .addContent(
                                LayoutElementBuilders.Spacer.Builder()
                                    .setHeight(DimensionBuilders.dp(8f))
                                    .build()
                            )
                            .addContent(
                                Text.Builder(context, "❤️ $heartRate")
                                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                                    .setColor(ColorBuilders.argb(0xFFCC0000.toInt()))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    } else {
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(backgroundColor))
                            .build()
                    )
                    .build()
            )
            .addContent(
                PrimaryLayout.Builder(buildDeviceParameters(context.resources))
                    .setResponsiveContentInsetEnabled(true)
                    .setContent(
                        LayoutElementBuilders.Column.Builder()
                            .addContent(
                                Text.Builder(context, time)
                                    .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                                    .setColor(ColorBuilders.argb(0xFF000000.toInt()))
                                    .build()
                            )
                            .addContent(
                                LayoutElementBuilders.Spacer.Builder()
                                    .setHeight(DimensionBuilders.dp(0f))
                                    .build()
                            )
                            .addContent(
                                Text.Builder(context, "$date ($weekday)")
                                    .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                    .setColor(ColorBuilders.argb(0xFF333333.toInt()))
                                    .build()
                            )
                            .addContent(
                                LayoutElementBuilders.Spacer.Builder()
                                    .setHeight(DimensionBuilders.dp(8f))
                                    .build()
                            )
                            .addContent(
                                Text.Builder(context, "❤️ $heartRate")
                                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                                    .setColor(ColorBuilders.argb(0xFFCC0000.toInt()))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }
}

@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable

fun TilePreview() {
    LayoutRootPreview(root = tileLayout(LocalContext.current, "75", backgroundColor = -1))
}

@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable

fun TileHighHeartRatePreview() {
    LayoutRootPreview(root = tileLayout(LocalContext.current, "105", 0xFFFF0000.toInt()))
}
