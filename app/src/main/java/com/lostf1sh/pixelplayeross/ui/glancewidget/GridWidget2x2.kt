// [Chomu] Modified: content scale and background opacity through WidgetFrame and scaled sizes.
package com.lostf1sh.pixelplayeross.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider // [Chomu]
import androidx.compose.ui.unit.DpSize // [Chomu]
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.lostf1sh.pixelplayeross.MainActivity
import com.lostf1sh.pixelplayeross.data.model.PlayerInfo

class GridWidget2x2 : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<PlayerInfo>()
            CompositionLocalProvider(LocalWidgetScale provides playerInfo.widgetScale) { // [Chomu] widget scale
                GlanceTheme {
                    GridWidget2x2Content(playerInfo = playerInfo, context = context)
                }
            }
        }
    }

    @Composable
    private fun GridWidget2x2Content(
        playerInfo: PlayerInfo,
        context: Context
    ) {
        val isPlaying = playerInfo.isPlaying
        val albumArtBitmapData = playerInfo.albumArtBitmapData
        val albumArtUri = playerInfo.albumArtUri

        val colors = playerInfo.getWidgetColors()

        val widgetCornerRadius = 28.dp

        val size = LocalSize.current
        val minSide = min(size.width, size.height)
        
        val dynamicIconSize = (minSide.value * 0.14f).dp
        val dynamicPlayIconSize = (minSide.value * 0.16f).dp
        val albumArtSize = (minSide.value * 0.40f).dp

        Box(
            modifier = GlanceModifier
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            // region [Chomu] Widget frame and scaled sizes (sdp/scaled)
            WidgetFrame(
                modifier = GlanceModifier.size(minSide),
                backgroundColor = colors.background,
                cornerRadius = widgetCornerRadius,
                padding = 8.dp,
                size = DpSize(minSide, minSide)
            ) {
                val itemCornerRadius = 16.sdp

                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth()
                    ) {
                        AlbumArtImage(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            bitmapData = albumArtBitmapData,
                            albumArtUri = albumArtUri,
                            size = albumArtSize.scaled,
                            context = context,
                            cornerRadius = itemCornerRadius
                        )

                        Spacer(GlanceModifier.width(6.sdp))

                        PlayPauseButton(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            isPlaying = isPlaying,
                            backgroundColor = colors.playPauseBackground,
                            iconColor = colors.playPauseIcon,
                            cornerRadius = itemCornerRadius,
                            iconSize = dynamicPlayIconSize.scaled
                        )
                    }

                    Spacer(GlanceModifier.height(6.sdp))

                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth()
                    ) {
                        PreviousButton(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            backgroundColor = colors.prevNextBackground,
                            iconColor = colors.prevNextIcon,
                            cornerRadius = itemCornerRadius,
                            iconSize = dynamicIconSize.scaled
                        )

                        Spacer(GlanceModifier.width(6.sdp))

                        NextButton(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            backgroundColor = colors.prevNextBackground,
                            iconColor = colors.prevNextIcon,
                            cornerRadius = itemCornerRadius,
                            iconSize = dynamicIconSize.scaled
                        )
                    }
                }
            }
            // endregion [Chomu]
        }
    }
}
