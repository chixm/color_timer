package com.chixm.colortimer.tile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
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
@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val heartRate = getHeartRate()
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
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) 
            != PackageManager.PERMISSION_GRANTED) {
            "No Perm"
        } else {
            // In a real app, you would fetch the last known heart rate from a database 
            // or Health Services. For this example, we use a placeholder.
            "72"
        }
    }
}

private fun tileLayout(context: Context, heartRate: String, backgroundColor: Int): LayoutElementBuilders.LayoutElement {
    val now = Date()
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(now)
    val weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(now)

    if (backgroundColor == -1) {
        // 現状のTile APIでは画像背景は未サポート。単色（薄青）で代用、またはBox重ね疑似グラデーション案を利用。
        return LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(0xFFADD8E6.toInt()))
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
