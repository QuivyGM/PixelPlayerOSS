// [Chomu] Modified: content scale, background opacity, text backdrop and fitting to small sizes.
package com.lostf1sh.pixelplayeross.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider // [Chomu]
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lostf1sh.pixelplayeross.MainActivity
import com.lostf1sh.pixelplayeross.data.model.PlayerInfo
import kotlin.text.ifEmpty

class BarWidget4x1 : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<PlayerInfo>()
            CompositionLocalProvider(LocalWidgetScale provides playerInfo.widgetScale) { // [Chomu] widget scale
                GlanceTheme {
                    BarWidget4x1Content(playerInfo = playerInfo, context = context)
                }
            }
        }
    }

    @Composable
    private fun BarWidget4x1Content(
        playerInfo: PlayerInfo,
        context: Context
    ) {
        val title = playerInfo.songTitle.ifEmpty { "PixelPlayerOSS" }
        val artist = playerInfo.artistName.ifEmpty { "Tap to open" }
        val isPlaying = playerInfo.isPlaying
        val albumArtBitmapData = playerInfo.albumArtBitmapData
        val albumArtUri = playerInfo.albumArtUri

        val colors = playerInfo.getWidgetColors()

        // region [Chomu] Widget frame, scaled sizes (sdp/ssp), text backdrop and small-size fitting
        WidgetFrame(
            modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            backgroundColor = colors.background,
            cornerRadius = 28.dp,
            padding = 8.dp
        ) {
            val albumArtCornerRadius = 16.sdp
            val playButtonCornerRadius = if (isPlaying) 16.sdp else 20.sdp
            val controlButtonCornerRadius = 16.sdp

            // The artwork and buttons shrink together when the widget is too short or too narrow
            // for them; the spacing and backdrop padding stay, and the text keeps at least 48 dp.
            val content = LocalWidgetContentSize.current
            val fixedWidth = 8.sdp + 12.sdp + 8.sdp + 6.sdp * 2 + 48.sdp
            val fit = minOf(1f, content.height / 44.sdp, (content.width - fixedWidth) / 164.sdp)
                .coerceAtLeast(0f)
            val albumArtSize = 44.sdp * fit
            val buttonSize = 40.sdp * fit
            val showArtist = textHeight(14.ssp) + 2.sdp + textHeight(12.ssp) <= albumArtSize

            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                WidgetTextBackdrop(
                    backdrop = colors.textBackdrop,
                    height = albumArtSize,
                    cornerRadius = albumArtCornerRadius,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    AlbumArtImage(
                        modifier = GlanceModifier.size(albumArtSize),
                        bitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        size = albumArtSize,
                        context = context,
                        cornerRadius = albumArtCornerRadius
                    )

                    Spacer(GlanceModifier.width(8.sdp))

                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontSize = 14.ssp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            ),
                            maxLines = 1
                        )
                        if (showArtist) {
                            Spacer(GlanceModifier.height(2.sdp))
                            Text(
                                text = artist,
                                style = TextStyle(
                                    fontSize = 12.ssp,
                                    color = colors.artist
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(GlanceModifier.width(8.sdp))

                PreviousButton(
                    modifier = GlanceModifier.size(buttonSize),
                    backgroundColor = colors.prevNextBackground,
                    iconColor = colors.prevNextIcon,
                    cornerRadius = controlButtonCornerRadius
                )

                Spacer(GlanceModifier.width(6.sdp))

                PlayPauseButton(
                    modifier = GlanceModifier.size(buttonSize),
                    isPlaying = isPlaying,
                    backgroundColor = colors.playPauseBackground,
                    iconColor = colors.playPauseIcon,
                    cornerRadius = playButtonCornerRadius
                )

                Spacer(GlanceModifier.width(6.sdp))

                NextButton(
                    modifier = GlanceModifier.size(buttonSize),
                    backgroundColor = colors.prevNextBackground,
                    iconColor = colors.prevNextIcon,
                    cornerRadius = controlButtonCornerRadius
                )
            }
        }
        // endregion [Chomu]
    }
}
