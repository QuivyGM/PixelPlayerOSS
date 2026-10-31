// [Chomu] Modified: fill mode, background opacity, text backdrop and fitting to small sizes.
// Every layout marked "[Chomu] Modified" below now draws through WidgetFrame and uses the
// fill-mode scaled units (sdp/ssp/scaled) in place of dp/sp.
package com.lostf1sh.pixelplayeross.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider // [Chomu]
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lostf1sh.pixelplayeross.MainActivity
import com.lostf1sh.pixelplayeross.data.model.PlayerInfo
import com.lostf1sh.pixelplayeross.R
import androidx.core.graphics.scale
import androidx.glance.unit.ColorProvider
import com.lostf1sh.pixelplayeross.data.model.QueueItem
import com.lostf1sh.pixelplayeross.utils.createScalableBackgroundBitmap
import java.util.Locale
import timber.log.Timber

class PixelPlayerGlanceWidget : GlanceAppWidget() {

    companion object {
        private val VERY_THIN_LAYOUT_SIZE = DpSize(width = 200.dp, height = 60.dp)
        private val THIN_LAYOUT_SIZE = DpSize(width = 250.dp, height = 80.dp)
        private val SMALL_HORIZONTAL_LAYOUT_SIZE = DpSize(width = 110.dp, height = 60.dp)
        private val ONE_BY_ONE_LAYOUT_SIZE = DpSize(width = 110.dp, height = 110.dp)
        private val GABE_LAYOUT_SIZE = DpSize(width = 110.dp, height = 220.dp)
        private val GABE_TWO_HEIGHT_LAYOUT_SIZE = DpSize(width = 110.dp, height = 200.dp)
        private val SMALL_LAYOUT_SIZE = DpSize(width = 120.dp, height = 100.dp)
        private val MEDIUM_LAYOUT_SIZE = DpSize(width = 250.dp, height = 150.dp)
        private val LARGE_LAYOUT_SIZE = DpSize(width = 300.dp, height = 180.dp)
        private val EXTRA_LARGE_LAYOUT_SIZE = DpSize(width = 300.dp, height = 220.dp)
        private val EXTRA_LARGE_PLUS_LAYOUT_SIZE = DpSize(width = 350.dp, height = 260.dp)
        private val HUGE_LAYOUT_SIZE = DpSize(width = 400.dp, height = 300.dp)
    }

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<PlayerInfo>()
            val currentSize = LocalSize.current

            Timber.tag("PixelPlayerGlanceWidget")
                .d("Providing Glance. PlayerInfo: title='${playerInfo.songTitle}', artist='${playerInfo.artistName}', isPlaying=${playerInfo.isPlaying}, hasBitmap=${playerInfo.albumArtBitmapData != null}, progress=${playerInfo.currentPositionMs}/${playerInfo.totalDurationMs}")

            // themeColors is null when there is no palette (nothing playing, or no
            // artwork), in which case the regular dynamic Glance theme applies.
            val albumColors = remember(playerInfo.themeColors) {
                playerInfo.themeColors?.toGlanceColorProviders()
            }
            CompositionLocalProvider(LocalWidgetFill provides playerInfo.fillMode) { // [Chomu] fill mode
                if (albumColors != null) {
                    GlanceTheme(colors = albumColors) {
                        WidgetUi(playerInfo = playerInfo, size = currentSize, context = context)
                    }
                } else {
                    GlanceTheme {
                        WidgetUi(playerInfo = playerInfo, size = currentSize, context = context)
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetUi(
        playerInfo: PlayerInfo,
        size: DpSize,
        context: Context
    ) {
        val title = playerInfo.songTitle.ifEmpty { context.getString(R.string.app_name) }
        val artist = playerInfo.artistName.ifEmpty { context.getString(R.string.widget_tap_to_open) }
        val isPlaying = playerInfo.isPlaying
        val isFavorite = playerInfo.isFavorite
        val albumArtBitmapData = playerInfo.albumArtBitmapData
        val albumArtUri = playerInfo.albumArtUri

        Timber.tag("PixelPlayerGlanceWidget")
            .d("WidgetUi: PlayerInfo received. Title: $title, Artist: $artist, HasBitmapData: ${albumArtBitmapData != null}, BitmapDataSize: ${albumArtBitmapData?.size ?: "N/A"}")

        val actualBackgroundColor = GlanceTheme.colors.surface
            .withOpacity(context, playerInfo.backgroundOpacity) // [Chomu] background opacity
        val backdrop = textBackdrop(GlanceTheme.colors.surface, playerInfo.backgroundOpacity) // [Chomu] text backdrop
        val onBackgroundColor = GlanceTheme.colors.onSurface

        val baseModifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>())

        Box(
            GlanceModifier.fillMaxSize()
        ) {
            val isOneColumn = size.width < SMALL_LAYOUT_SIZE.width
            val isSmallHeight = size.height < SMALL_LAYOUT_SIZE.height

            if (isOneColumn) {
                when {
                    size.height <= ONE_BY_ONE_LAYOUT_SIZE.height -> OneByOneWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        isPlaying = isPlaying
                    )
                    size.height <= GABE_TWO_HEIGHT_LAYOUT_SIZE.height -> GabeTwoHeightWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        context = context
                    )
                    else -> GabeWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 360.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        context = context
                    )
                }
            } else if (isSmallHeight) {
                when {
                    size.width < VERY_THIN_LAYOUT_SIZE.width -> SmallHorizontalWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        context = context
                    )
                    size.width < THIN_LAYOUT_SIZE.width -> VeryThinWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        textColor = onBackgroundColor,
                        textBackdrop = backdrop, // [Chomu]
                        context = context,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp
                    )
                    else -> ThinWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        textColor = onBackgroundColor,
                        textBackdrop = backdrop, // [Chomu]
                        context = context
                    )
                }
            } else {
                when {
                    size.width < MEDIUM_LAYOUT_SIZE.width || size.height < MEDIUM_LAYOUT_SIZE.height -> SmallWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        context = context
                    )
                    size.width < LARGE_LAYOUT_SIZE.width || size.height < LARGE_LAYOUT_SIZE.height -> MediumWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        textColor = onBackgroundColor,
                        textBackdrop = backdrop, // [Chomu]
                        context = context,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp
                    )
                    size.width < EXTRA_LARGE_LAYOUT_SIZE.width || size.height < EXTRA_LARGE_LAYOUT_SIZE.height -> LargeWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        isPlaying = isPlaying,
                        isFavorite = isFavorite,
                        textColor = onBackgroundColor,
                        textBackdrop = backdrop, // [Chomu]
                        context = context
                    )
                    else -> ExtraLargeWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        isPlaying = isPlaying,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        textColor = onBackgroundColor,
                        context = context,
                        textBackdrop = backdrop, // [Chomu]
                        queue = playerInfo.queue
                    )
                }
            }
        }
    }

    // [Chomu] Modified: WidgetFrame, scaled sizes, text backdrop and fitting to the content size.
    @Composable
    fun VeryThinWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        artist: String,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        textBackdrop: TextBackdrop, // [Chomu]
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 10.dp
        ) {
            // Fit the artwork and buttons to the space so short or narrow widgets don't overflow.
            val content = LocalWidgetContentSize.current
            val buttonSize = minOf(48.sdp, content.height)
            val controlsWidth = 8.sdp + buttonSize + 10.sdp + buttonSize
            val albumArtSize = minOf(content.height, (content.width - controlsWidth) / 2).coerceAtLeast(0.dp)
            val showArtist = textHeight(16.ssp) + textHeight(14.ssp) <= albumArtSize
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(bgCornerRadius),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {

                WidgetTextBackdrop(
                    backdrop = textBackdrop,
                    height = albumArtSize,
                    cornerRadius = bgCornerRadius,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    AlbumArtImageGlance(
                        modifier = GlanceModifier
                            .size(albumArtSize),
                        bitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        context = context,
                        cornerRadius = bgCornerRadius
                    )
                    Spacer(GlanceModifier.width(10.sdp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(text = title, style = TextStyle(fontSize = 16.ssp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                        if (showArtist && artist.isNotEmpty() && artist != context.getString(R.string.widget_tap_to_open)) {
                            Text(text = artist, style = TextStyle(fontSize = 14.ssp, color = textColor), maxLines = 1)
                        }
                    }
                }
                Spacer(GlanceModifier.width(8.sdp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .size(buttonSize, buttonSize)
                        .fillMaxHeight(),
                    backgroundColor = primaryContainerColor,
                    iconColor = onPrimaryContainerColor,
                    isPlaying = isPlaying,
                    iconSize = 26.sdp,
                    cornerRadius = 10.sdp
                )
                Spacer(GlanceModifier.width(10.sdp))
                NextButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .size(buttonSize, buttonSize)
                        .fillMaxHeight(),
                    iconColor = onSecondaryColor,
                    iconSize = 26.sdp,
                    backgroundColor = secondaryColor,
                    cornerRadius = 10.sdp
                )
            }
        }
    }


    // [Chomu] Modified: WidgetFrame, scaled sizes, text backdrop and fitting to the content size.
    @Composable
    fun ThinWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        title: String,
        artist: String,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        textBackdrop: TextBackdrop, // [Chomu]
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 10.dp
        ) {
            // Fit the artwork and buttons to the space so short or narrow widgets don't overflow.
            val content = LocalWidgetContentSize.current
            val buttonSize = minOf(48.sdp, content.height)
            val controlsWidth = 8.sdp + buttonSize + 10.sdp + buttonSize
            val albumArtSize = minOf(content.height, (content.width - controlsWidth) / 2).coerceAtLeast(0.dp)
            val showArtist = textHeight(16.ssp) + textHeight(14.ssp) <= albumArtSize
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(bgCornerRadius)
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {

                WidgetTextBackdrop(
                    backdrop = textBackdrop,
                    height = albumArtSize,
                    cornerRadius = bgCornerRadius,
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    AlbumArtImageGlance(
                        modifier = GlanceModifier
                            .size(albumArtSize),
                        bitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        context = context,
                        cornerRadius = bgCornerRadius
                    )
                    Spacer(GlanceModifier.width(14.sdp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(text = title, style = TextStyle(fontSize = 16.ssp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                        if (showArtist && artist.isNotEmpty() && artist != context.getString(R.string.widget_tap_to_open)) {
                            Text(text = artist, style = TextStyle(fontSize = 14.ssp, color = textColor), maxLines = 1)
                        }
                    }
                }
                Spacer(GlanceModifier.width(8.sdp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .size(buttonSize, buttonSize)
                        .fillMaxHeight(),
                    backgroundColor = primaryContainerColor,
                    iconColor = onPrimaryContainerColor,
                    isPlaying = isPlaying,
                    iconSize = 26.sdp,
                    cornerRadius = 10.sdp
                )
                Spacer(GlanceModifier.width(10.sdp))
                NextButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .size(buttonSize, buttonSize)
                        .fillMaxHeight(),
                    iconColor = onSecondaryColor,
                    iconSize = 26.sdp,
                    backgroundColor = secondaryColor,
                    cornerRadius = 10.sdp
                )
            }
        }
    }

    // [Chomu] Modified: WidgetFrame and scaled sizes.
    @Composable
    fun GabeTwoHeightWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 10.dp
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {

                AlbumArtImageGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .height(48.sdp)
                    ,
                    bitmapData = albumArtBitmapData,
                    albumArtUri = albumArtUri,
                    context = context,
                    cornerRadius = 64.sdp
                )
                Spacer(GlanceModifier.height(14.sdp))
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(bgCornerRadius)
                ) {
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        backgroundColor = primaryContainerColor,
                        iconColor = onPrimaryContainerColor,
                        isPlaying = isPlaying,
                        iconSize = 26.sdp,
                        cornerRadius = 10.sdp
                    )
                    Spacer(GlanceModifier.height(10.sdp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        iconColor = onSecondaryColor,
                        iconSize = 26.sdp,
                        backgroundColor = secondaryColor,
                        cornerRadius = 10.sdp
                    )
                }
            }
        }
    }

    // [Chomu] Modified: WidgetFrame and scaled sizes.
    @Composable
    fun GabeWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 10.dp
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .height(48.sdp),
                    bitmapData = albumArtBitmapData,
                    albumArtUri = albumArtUri,
                    context = context,
                    cornerRadius = 64.sdp
                )
                Spacer(GlanceModifier.height(14.sdp))
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(bgCornerRadius)
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        iconColor = onSecondaryColor,
                        iconSize = 26.sdp,
                        backgroundColor = secondaryColor,
                        cornerRadius = 10.sdp
                    )
                    Spacer(GlanceModifier.height(10.sdp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        backgroundColor = primaryContainerColor,
                        iconColor = onPrimaryContainerColor,
                        isPlaying = isPlaying,
                        iconSize = 26.sdp,
                        cornerRadius = 10.sdp
                    )
                    Spacer(GlanceModifier.height(10.sdp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        iconColor = onSecondaryColor,
                        iconSize = 26.sdp,
                        backgroundColor = secondaryColor,
                        cornerRadius = 10.sdp
                    )
                }
            }
        }
    }

    // [Chomu] Modified: WidgetFrame and scaled sizes.
    @Composable
    fun OneByOneWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        isPlaying: Boolean
    ) {
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 30.dp,
            contentAlignment = Alignment.Center
        ) {
            PlayPauseButtonGlance(
                modifier = GlanceModifier.fillMaxSize(),
                backgroundColor = primaryContainerColor,
                iconColor = onPrimaryContainerColor,
                isPlaying = isPlaying,
                iconSize = 36.sdp,
                cornerRadius = 30.sdp
            )
        }
    }

    // [Chomu] Modified: WidgetFrame, scaled sizes and fitting to the content size.
    @Composable
    fun SmallHorizontalWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        context: Context
    ) {
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 10.dp
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(bgCornerRadius),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier.padding(vertical = 6.sdp),
                    bitmapData = albumArtBitmapData,
                    albumArtUri = albumArtUri,
                    // At most what fits the height with its padding, leaving half the width to the button.
                    size = minOf(
                        58.sdp,
                        LocalWidgetContentSize.current.height - 12.sdp,
                        (LocalWidgetContentSize.current.width - 14.sdp) / 2
                    ).coerceAtLeast(0.dp),
                    context = context,
                    cornerRadius = 64.sdp
                )
                Spacer(GlanceModifier.width(14.sdp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                    backgroundColor = primaryContainerColor,
                    iconColor = onPrimaryContainerColor,
                    isPlaying = isPlaying,
                    iconSize = 26.sdp,
                    cornerRadius = 10.sdp
                )
            }
        }
    }

    // [Chomu] Modified: WidgetFrame, scaled sizes and fitting to the content size.
    @Composable
    fun SmallWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer
        val buttonCornerRadius = 16.dp
        val playButtonCornerRadius = if (isPlaying) 12.dp else 60.dp

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 12.dp,
            contentCornerRadius = 16.dp
        ) {
            // The two button rows share the height with the artwork, which keeps at least a third.
            val buttonHeight = minOf(
                50.sdp,
                (LocalWidgetContentSize.current.height - 16.sdp) / 3
            ).coerceAtLeast(0.dp)
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    AlbumArtImageGlance(
                        modifier = GlanceModifier.defaultWeight(),
                        bitmapData = albumArtBitmapData,
                        albumArtUri = albumArtUri,
                        context = context,
                        cornerRadius = 64.sdp
                    )
                }
                Spacer(GlanceModifier.height(8.sdp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .height(buttonHeight),
                    isPlaying = isPlaying,
                    cornerRadius = playButtonCornerRadius,
                    iconSize = 26.sdp,
                    backgroundColor = primaryContainerColor,
                    iconColor = onPrimaryContainerColor
                )
                Spacer(GlanceModifier.height(8.sdp))
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .height(buttonHeight),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconSize = 26.sdp,
                        cornerRadius = buttonCornerRadius,
                        backgroundColor = secondaryColor,
                        iconColor = onSecondaryColor
                    )
                    Spacer(GlanceModifier.width(8.sdp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconSize = 26.sdp,
                        cornerRadius = buttonCornerRadius,
                        backgroundColor = secondaryColor,
                        iconColor = onSecondaryColor
                    )
                }
            }
        }
    }

    // [Chomu] Modified: WidgetFrame, scaled sizes, text backdrop and title line fitting.
    @Composable
    fun MediumWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        artist: String,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        textBackdrop: TextBackdrop, // [Chomu]
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer
        val buttonCornerRadius = 60.dp
        val playButtonCornerRadius = if (isPlaying) 14.dp else 60.dp

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 16.dp
        ) {
            // With large fonts a two-line title and the artist would overflow the 80 dp backdrop.
            val titleLines = if (textHeight(16.ssp, lines = 2) + 4.sdp + textHeight(13.ssp) <= 80.sdp) 2 else 1
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // The artist is kept to one line so both fit the backdrop, which matches the artwork height.
                    WidgetTextBackdrop(
                        backdrop = textBackdrop,
                        height = 80.sdp,
                        cornerRadius = 16.sdp,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        AlbumArtImageGlance(
                            bitmapData = albumArtBitmapData,
                            albumArtUri = albumArtUri,
                            size = 80.sdp,
                            context = context,
                            cornerRadius = 16.sdp
                        )
                        Spacer(GlanceModifier.width(12.sdp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = title,
                                style = TextStyle(
                                    fontSize = 16.ssp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                ),
                                maxLines = titleLines
                            )
                            Spacer(GlanceModifier.height(4.sdp))
                            Text(
                                text = artist,
                                style = TextStyle(fontSize = 13.ssp, color = textColor),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(GlanceModifier.height(12.sdp))

                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        cornerRadius = buttonCornerRadius
                    )
                    Spacer(GlanceModifier.width(8.sdp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = onPrimaryContainerColor,
                        backgroundColor = primaryContainerColor,
                        cornerRadius = playButtonCornerRadius
                    )
                    Spacer(GlanceModifier.width(8.sdp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        cornerRadius = buttonCornerRadius
                    )
                }
            }
        }
    }

    // [Chomu] Modified: WidgetFrame, scaled sizes and text backdrop (favorite button moved onto it).
    @Composable
    fun LargeWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        artist: String,
        albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        isPlaying: Boolean,
        isFavorite: Boolean,
        textColor: ColorProvider,
        textBackdrop: TextBackdrop, // [Chomu]
        context: Context
    ) {
        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 18.dp
        ) {
            Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.sdp)) {
                    WidgetTextBackdrop(
                        backdrop = textBackdrop,
                        height = 64.sdp,
                        cornerRadius = 18.sdp,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        AlbumArtImageGlance(
                            bitmapData = albumArtBitmapData,
                            albumArtUri = albumArtUri,
                            size = 64.sdp,
                            context = context,
                            cornerRadius = 18.sdp
                        )
                        Spacer(GlanceModifier.width(12.sdp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(text = title, style = TextStyle(fontSize = 16.ssp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                            Text(text = artist, style = TextStyle(fontSize = 13.ssp, color = textColor), maxLines = 1)
                        }
                        Spacer(GlanceModifier.width(4.sdp))
                        Image(
                            provider = ImageProvider(if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24),
                            contentDescription = context.getString(R.string.cd_favorite),
                            modifier = GlanceModifier
                                .size(28.sdp)
                                .clickable(actionRunCallback<PlayerControlActionCallback>(actionParametersOf(PlayerActions.key to PlayerActions.FAVORITE)))
                                .padding(2.sdp),
                            colorFilter = ColorFilter.tint(textColor)
                        )
                    }
                }
                Spacer(GlanceModifier.height(4.sdp))
                Spacer(GlanceModifier.height(10.sdp))

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val secondaryColor = GlanceTheme.colors.secondaryContainer
                    val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
                    val primaryContainerColor = GlanceTheme.colors.primaryContainer
                    val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer
                    val buttonCornerRadius = 60.sdp
                    val playButtonCornerRadius = if (isPlaying) 14.sdp else 60.sdp

                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        cornerRadius = buttonCornerRadius
                    )
                    Spacer(GlanceModifier.width(8.sdp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = onPrimaryContainerColor,
                        backgroundColor = primaryContainerColor,
                        cornerRadius = playButtonCornerRadius
                    )
                    Spacer(GlanceModifier.width(8.sdp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        cornerRadius = buttonCornerRadius
                    )
                }
            }
        }
    }

    // [Chomu] Modified: WidgetFrame, scaled sizes, text backdrop and hiding the queue when short.
    @Composable
    fun ExtraLargeWidgetLayout(
        modifier: GlanceModifier, title: String, artist: String, albumArtBitmapData: ByteArray?,
        albumArtUri: String?,
        isPlaying: Boolean, backgroundColor: ColorProvider, bgCornerRadius: Dp,
        textColor: ColorProvider,
        context: Context,
        textBackdrop: TextBackdrop, // [Chomu]
        queue: List<QueueItem>
    ) {
        val playButtonCornerRadius = if (isPlaying) 16.dp else 60.dp

        WidgetFrame(
            modifier = modifier,
            backgroundColor = backgroundColor,
            cornerRadius = bgCornerRadius,
            padding = 16.dp,
            contentCornerRadius = 14.dp
        ) {
            // Header, buttons and queue with their spacing; without room for all, the queue is left out.
            val showQueue = LocalWidgetContentSize.current.height >= 210.sdp
            Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    // The title is kept to one line so it fits the backdrop, which matches the artwork height.
                    WidgetTextBackdrop(
                        backdrop = textBackdrop,
                        height = 68.sdp,
                        cornerRadius = 16.sdp,
                        modifier = GlanceModifier.defaultWeight()
                    ) {
                        AlbumArtImageGlance(
                            bitmapData = albumArtBitmapData,
                            albumArtUri = albumArtUri,
                            size = 68.sdp,
                            context = context,
                            cornerRadius = 16.sdp
                        )
                        Spacer(GlanceModifier.width(16.sdp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = title,
                                style = TextStyle(fontSize = 20.ssp, fontWeight = FontWeight.Bold, color = textColor),
                                maxLines = 1
                            )
                            Text(
                                text = artist,
                                style = TextStyle(fontSize = 16.ssp, color = textColor),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(GlanceModifier.height(14.sdp))

                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .height(56.sdp)
                    ,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val secondaryColor = GlanceTheme.colors.secondaryContainer
                    val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
                    val primaryContainerColor = GlanceTheme.colors.primaryContainer
                    val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer
                    val buttonCornerRadius = 60.sdp

                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        iconSize = 28.sdp,
                        cornerRadius = buttonCornerRadius
                    )
                    Spacer(GlanceModifier.width(10.sdp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = onPrimaryContainerColor,
                        backgroundColor = primaryContainerColor,
                        iconSize = 30.sdp,
                        cornerRadius = playButtonCornerRadius
                    )
                    Spacer(GlanceModifier.width(10.sdp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        iconSize = 28.sdp,
                        cornerRadius = buttonCornerRadius
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                if (showQueue) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.sdp)
                            .background(textColor.getColor(context).copy(alpha = 0.15f))
                            .height(2.sdp)
                            .cornerRadius(60.sdp)
                    ) {

                    }

                    Spacer(GlanceModifier.height(12.sdp))

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(58.sdp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val items = queue.take(4)
                        val itemSize = 58.sdp
                        val cornerRadius = 14.sdp

                        for (i in 0 until 4) {
                            Box(
                                modifier = GlanceModifier.defaultWeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (i < items.size) {
                                    val queueItem = items[i]
                                    AlbumArtImageGlance(
                                        modifier = GlanceModifier.clickable(
                                            actionRunCallback<PlayerControlActionCallback>(
                                                actionParametersOf(
                                                    PlayerActions.key to PlayerActions.PLAY_FROM_QUEUE,
                                                    PlayerActions.songIdKey to queueItem.id
                                                )
                                            )
                                        ),
                                        bitmapData = null,
                                        albumArtUri = queueItem.albumArtUri,
                                        size = itemSize,
                                        context = context,
                                        cornerRadius = cornerRadius
                                    )
                                } else {
                                    EndOfQueuePlaceholder(
                                        size = itemSize,
                                        cornerRadius = cornerRadius
                                    )
                                }
                            }

                            if (i < 3) {
                                Spacer(GlanceModifier.width(8.sdp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AlbumArtImageGlance(
        bitmapData: ByteArray?,
        albumArtUri: String? = null,
        size: Dp? = null,
        context: Context,
        modifier: GlanceModifier = GlanceModifier,
        cornerRadius: Dp = 16.dp
    ) {
        val TAG_AAIG = "AlbumArtImageGlance"
        Timber.tag(TAG_AAIG)
            .d("Init. bitmapData is null: ${bitmapData == null}. Requested Dp size: $size")
        if (bitmapData != null) Timber.tag(TAG_AAIG).d("bitmapData size: ${bitmapData.size} bytes")

        val sizingModifier = if (size != null) modifier.size(size) else modifier
        val widgetDpSize = LocalSize.current

        val imageProvider = bitmapData?.let { data ->
            val cacheKey = AlbumArtBitmapCache.getKey(data)
            var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)

            if (bitmap != null) {
                Timber.tag(TAG_AAIG).d("Bitmap cache HIT for key: $cacheKey. Using cached bitmap.")
            } else {
                Timber.tag(TAG_AAIG).d("Bitmap cache MISS for key: $cacheKey. Decoding new bitmap.")
                try {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(data, 0, data.size, options)
                    Timber.tag(TAG_AAIG)
                        .d("Initial bounds: ${options.outWidth}x${options.outHeight}")

                    val imageHeight = options.outHeight
                    val imageWidth = options.outWidth
                    var inSampleSize = 1

                    val targetWidthPx: Int
                    val targetHeightPx: Int
                    with(context.resources.displayMetrics) {
                        if (size != null) {
                            // [Chomu] At least 1 px: layouts can shrink the artwork to nothing, and 0 would never stop halving.
                            val targetSizePx = (size.value * density).toInt().coerceAtLeast(1)
                            targetWidthPx = targetSizePx
                            targetHeightPx = targetSizePx
                            Timber.tag(TAG_AAIG).d("Target Px size for Dp $size: $targetSizePx")
                        } else {
                            targetWidthPx = (widgetDpSize.width.value * density).toInt()
                            targetHeightPx = (widgetDpSize.height.value * density).toInt()
                            Timber.tag(TAG_AAIG).d("Target Px size from widget DpSize ${widgetDpSize}: ${targetWidthPx}x${targetHeightPx}")
                        }
                    }

                    if (imageHeight > targetHeightPx || imageWidth > targetWidthPx) {
                        val halfHeight: Int = imageHeight / 2
                        val halfWidth: Int = imageWidth / 2
                        while (halfHeight / inSampleSize >= targetHeightPx && halfWidth / inSampleSize >= targetWidthPx) {
                            inSampleSize *= 2
                        }
                    }
                    Timber.tag(TAG_AAIG).d("Calculated inSampleSize: $inSampleSize")

                    options.inSampleSize = inSampleSize
                    options.inJustDecodeBounds = false
                    val sampledBitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

                    if (sampledBitmap == null) {
                        Timber.tag(TAG_AAIG)
                            .e("BitmapFactory.decodeByteArray returned null after sampling.")
                        return@let null
                    }
                    Timber.tag(TAG_AAIG)
                        .d("Sampled bitmap size: ${sampledBitmap.width}x${sampledBitmap.height}")

                    bitmap = sampledBitmap

                    Timber.tag(TAG_AAIG)
                        .d("Final bitmap size: ${bitmap.width}x${bitmap.height}. Putting into cache with key: $cacheKey")
                    bitmap.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }

                } catch (e: Exception) {
                    Timber.tag(TAG_AAIG).e(e, "Error decoding or scaling bitmap: ${e.message}")
                    bitmap = null
                }
            }
            bitmap?.let { ImageProvider(it) }
        } ?: albumArtUri?.let { rawUri ->
            val cacheKey = "uri:$rawUri"
            var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)
            if (bitmap == null) {
                bitmap = decodeAlbumArtFromUri(
                    context = context,
                    rawUri = rawUri,
                    requestedSize = size,
                    widgetDpSize = widgetDpSize
                )
                bitmap?.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }
            }
            bitmap?.let { ImageProvider(it) }
        }

        Box(
            modifier = sizingModifier
        ) {
            if (imageProvider != null) {
                Image(
                    provider = imageProvider,
                    contentDescription = context.getString(R.string.widget_album_art),
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(cornerRadius),
                    contentScale = ContentScale.Crop
                )
            } else {
                val placeholderBaseSize = size ?: minOf(widgetDpSize.width, widgetDpSize.height)
                val resolvedPlaceholderBaseSize = when {
                    placeholderBaseSize == Dp.Unspecified || placeholderBaseSize <= 0.dp -> 72.dp
                    else -> placeholderBaseSize
                }

                val placeholderSize = resolvedPlaceholderBaseSize
                    .times(0.65f)
                    .coerceIn(36.dp, 96.dp)

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(cornerRadius)
                        .background(GlanceTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_placeholder),
                        contentDescription = context.getString(R.string.widget_album_art_placeholder),
                        modifier = GlanceModifier.size(placeholderSize),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }
        }
    }

    private fun decodeAlbumArtFromUri(
        context: Context,
        rawUri: String,
        requestedSize: Dp?,
        widgetDpSize: DpSize
    ): Bitmap? {
        val (targetWidthPx, targetHeightPx) = with(context.resources.displayMetrics) {
            if (requestedSize != null) {
                val target = (requestedSize.value * density).toInt().coerceAtLeast(1)
                target to target
            } else {
                val width = (widgetDpSize.width.value * density).toInt().coerceAtLeast(1)
                val height = (widgetDpSize.height.value * density).toInt().coerceAtLeast(1)
                width to height
            }
        }
        return decodeWidgetAlbumArtBitmap(
            context = context,
            rawUri = rawUri,
            targetWidthPx = targetWidthPx,
            targetHeightPx = targetHeightPx,
        )
    }

    // [Chomu] Modified: default sizes scaled in fill mode (sdp).
    @Composable
    fun PlayPauseButtonGlance(
        modifier: GlanceModifier = GlanceModifier,
        isPlaying: Boolean,
        iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
        backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
        iconSize: Dp = 24.sdp,
        cornerRadius: Dp = 0.dp
    ) {
        val context = LocalContext.current
        val params = actionParametersOf(PlayerActions.key to PlayerActions.PLAY_PAUSE)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(if (isPlaying) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24),
                contentDescription = context.getString(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    // [Chomu] Modified: default sizes scaled in fill mode (sdp).
    @Composable
    fun NextButtonDGlance(
        modifier: GlanceModifier = GlanceModifier,
        width: Dp? = null,
        height: Dp? = null,
        backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
        iconProvider: ImageProvider = ImageProvider(R.drawable.rounded_skip_next_24),
        iconSize: Dp = 24.sdp,
        iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
        topLeftCorner: Dp = 8.dp,
        topRightCorner: Dp = 8.dp,
        bottomLeftCorner: Dp = 8.dp,
        bottomRightCorner: Dp = 8.dp,
    ) {
        val context = LocalContext.current
        val bgColor = backgroundColor.getColor(context)
        val params = actionParametersOf(PlayerActions.key to PlayerActions.NEXT)

        val sizeModifier = modifier.then(
            when {
                width != null && height != null -> GlanceModifier.size(width, height)
                width != null -> GlanceModifier.width(width)
                height != null -> GlanceModifier.height(height)
                else -> GlanceModifier
            }
        )

        val backgroundBitmap = createScalableBackgroundBitmap(
            context = context,
            color = bgColor,
            topLeft = topLeftCorner,
            topRight = topRightCorner,
            bottomLeft = bottomLeftCorner,
            bottomRight = bottomRightCorner,
            width = width,
            height = height
        )

        Box(
            modifier = sizeModifier
                .background(ImageProvider(backgroundBitmap))
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = iconProvider,
                contentDescription = context.getString(R.string.next_track),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    // [Chomu] Modified: default sizes scaled in fill mode (sdp).
    @Composable
    fun NextButtonGlance(
        modifier: GlanceModifier = GlanceModifier,
        iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
        backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
        iconSize: Dp = 24.sdp,
        cornerRadius: Dp = 8.sdp
    ) {
        val context = LocalContext.current
        val params = actionParametersOf(PlayerActions.key to PlayerActions.NEXT)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.rounded_skip_next_24),
                contentDescription = context.getString(R.string.next_track),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    // [Chomu] Modified: default sizes scaled in fill mode (sdp).
    @Composable
    fun PreviousButtonGlance(
        modifier: GlanceModifier = GlanceModifier,
        iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
        backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
        iconSize: Dp = 24.sdp,
        cornerRadius: Dp = 8.sdp
    ) {
        val context = LocalContext.current
        val params = actionParametersOf(PlayerActions.key to PlayerActions.PREVIOUS)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.rounded_skip_previous_24),
                contentDescription = context.getString(R.string.previous_track),
                modifier = GlanceModifier.size(iconSize),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    @Composable
    fun EndOfQueuePlaceholder(
        modifier: GlanceModifier = GlanceModifier,
        size: Dp,
        cornerRadius: Dp
    ) {
        Box(
            modifier = modifier
                .size(size)
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(cornerRadius)
        ) {

        }
    }
}

private fun formatDurationGlance(millis: Long): String {
    if (millis < 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
