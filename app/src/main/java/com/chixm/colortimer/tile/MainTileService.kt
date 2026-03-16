package com.chixm.colortimer.tile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.wear.tiles.ColorBuilders
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ModifiersBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.Typography
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.android.horologist.compose.tools.LayoutRootPreview
import com.google.android.horologist.compose.tools.buildDeviceParameters
import com.google.android.horologist.tiles.CoroutinesTileService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val RESOURCES_VERSION = "0"

/**
 * Tile that shows time and heart rate on a light blue background.
 */
class MainTileService : CoroutinesTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val heartRate = getHeartRate()
        
        val singleTileTimeline = TimelineBuilders.Timeline.Builder().addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder().setLayout(
                LayoutElementBuilders.Layout.Builder().setRoot(tileLayout(this, heartRate)).build()
            ).build()
        ).build()

        return TileBuilders.Tile.Builder().setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(singleTileTimeline).build()
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

private fun tileLayout(context: Context, heartRate: String): LayoutElementBuilders.LayoutElement {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
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

@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun TilePreview() {
    LayoutRootPreview(root = tileLayout(LocalContext.current, "75"))
}
