package com.lostf1sh.pixelplayeross.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.lostf1sh.pixelplayeross.R
import timber.log.Timber

/**
 * Outer container of a widget layout: the rounded background with [padding] around the
 * content. It is marked as the widget background, so Android 12+ launchers clip it and
 * animate app launches from it consistently.
 *
 * In fill mode ([LocalWidgetFill]) the background stays and the content is scaled by
 * [fillScale] into part of the padding: by at most [MAX_FILL_SCALE], and never closer to
 * the outline than its outer corners, rounded by [contentCornerRadius], allow plus
 * [FILL_SAFETY_MARGIN]. Widgets smaller than [MIN_FILL_SIZE] are not scaled. Sizes inside
 * should use [sdp], [ssp] and [scaled] to follow the scale, and fit fixed sizes to
 * [LocalWidgetContentSize]. [size] is the area the frame covers.
 */
@Composable
fun WidgetFrame(
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    cornerRadius: Dp,
    padding: Dp,
    contentCornerRadius: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    size: DpSize = LocalSize.current,
    content: @Composable () -> Unit
) {
    val borderRadius = widgetBorderRadius(LocalContext.current, cornerRadius, size)
    val scale = if (LocalWidgetFill.current && canFill(size)) {
        fillScale(size, padding, fillInset(borderRadius, contentCornerRadius) + FILL_SAFETY_MARGIN)
    } else {
        1f
    }
    val horizontalPadding = fillPadding(size.width, padding, scale)
    val verticalPadding = fillPadding(size.height, padding, scale)
    val contentSize = DpSize(
        width = (size.width - horizontalPadding * 2).coerceAtLeast(0.dp),
        height = (size.height - verticalPadding * 2).coerceAtLeast(0.dp)
    )
    Box(
        modifier = modifier
            .appWidgetBackground()
            .cornerRadius(borderRadius)
            .background(backgroundColor)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = contentAlignment
    ) {
        CompositionLocalProvider(
            LocalWidgetScale provides scale,
            LocalWidgetContentSize provides contentSize
        ) {
            content()
        }
    }
}

/**
 * Row holding the artwork and the title and artist, on a plate that keeps the text
 * readable when the widget background is translucent. The plate starts behind the
 * artwork and fades out towards the end of the text. It has a fixed [height] so it lines
 * up with the buttons beside it; [cornerRadius] should match the artwork's.
 */
@Composable
fun WidgetTextBackdrop(
    backdrop: TextBackdrop,
    height: Dp,
    cornerRadius: Dp,
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable RowScope.() -> Unit
) {
    var plateModifier = modifier
        .height(height)
        .cornerRadius(cornerRadius)
    if (backdrop.isVisible) {
        // The mask runs left to right, so flip it to keep the solid end behind the artwork in RTL.
        val isRtl = LocalContext.current.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val mask = if (isRtl) {
            horizontalGradientMask(backdrop.endOpacity, backdrop.startOpacity)
        } else {
            horizontalGradientMask(backdrop.startOpacity, backdrop.endOpacity)
        }
        plateModifier = plateModifier.background(
            imageProvider = ImageProvider(mask),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(backdrop.color)
        )
    }
    Row(
        modifier = plateModifier.padding(end = 12.sdp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun AlbumArtImage(
    modifier: GlanceModifier = GlanceModifier,
    bitmapData: ByteArray?,
    albumArtUri: String? = null,
    size: Dp,
    context: Context,
    cornerRadius: Dp
) {
    val imageProvider = bitmapData?.let { data ->
        val cacheKey = AlbumArtBitmapCache.getKey(data)
        var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)

        if (bitmap == null) {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(data, 0, data.size, options)

                var inSampleSize = 1
                // At least 1 px: layouts can shrink the artwork to nothing, and 0 would never stop halving.
                val targetSizePx = (size.value * context.resources.displayMetrics.density).toInt()
                    .coerceAtLeast(1)

                if (options.outHeight > targetSizePx || options.outWidth > targetSizePx) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while (halfHeight / inSampleSize >= targetSizePx &&
                        halfWidth / inSampleSize >= targetSizePx
                    ) {
                        inSampleSize *= 2
                    }
                }

                options.inSampleSize = inSampleSize
                options.inJustDecodeBounds = false
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

                bitmap?.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }
            } catch (e: Exception) {
                Timber.tag("WidgetImage").e(e, "Error decoding bitmap")
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
                requestedSize = size
            )
            bitmap?.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }
        }
        bitmap?.let { ImageProvider(it) }
    }

    val cdAlbumArt = context.getString(R.string.widget_album_art)
    val cdPlaceholder = context.getString(R.string.widget_album_art_placeholder)
    Box(
        modifier = modifier
    ) {
        if (imageProvider != null) {
            Image(
                provider = imageProvider,
                contentDescription = cdAlbumArt,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(cornerRadius),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(cornerRadius)
                    .background(GlanceTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_music_placeholder),
                    contentDescription = cdPlaceholder,
                    modifier = GlanceModifier.size(size * 0.6f),
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
    requestedSize: Dp,
): Bitmap? {
    val targetPx = (requestedSize.value * context.resources.displayMetrics.density)
        .toInt()
        .coerceAtLeast(64)
    return decodeWidgetAlbumArtBitmap(
        context = context,
        rawUri = rawUri,
        targetWidthPx = targetPx,
        targetHeightPx = targetPx,
    )
}

@Composable
fun WidgetIconButton(
    modifier: GlanceModifier,
    action: Action,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    imageProvider: ImageProvider,
    contentDescription: String,
    iconSize: Dp = 20.sdp,
    cornerRadius: Dp
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .cornerRadius(cornerRadius)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = imageProvider,
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(iconColor)
        )
    }
}

@Composable
fun PreviousButton(
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    cornerRadius: Dp,
    iconSize: Dp = 20.sdp
) {
    val context = LocalContext.current
    val params = actionParametersOf(PlayerActions.key to PlayerActions.PREVIOUS)
    WidgetIconButton(
        modifier = modifier,
        action = actionRunCallback<PlayerControlActionCallback>(params),
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        imageProvider = ImageProvider(R.drawable.rounded_skip_previous_filled_24),
        contentDescription = context.getString(R.string.previous_track),
        iconSize = iconSize,
        cornerRadius = cornerRadius
    )
}

@Composable
fun NextButton(
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    cornerRadius: Dp,
    iconSize: Dp = 20.sdp
) {
    val context = LocalContext.current
    val params = actionParametersOf(PlayerActions.key to PlayerActions.NEXT)
    WidgetIconButton(
        modifier = modifier,
        action = actionRunCallback<PlayerControlActionCallback>(params),
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        imageProvider = ImageProvider(R.drawable.rounded_skip_next_filled_24),
        contentDescription = context.getString(R.string.next_track),
        iconSize = iconSize,
        cornerRadius = cornerRadius
    )
}

@Composable
fun PlayPauseButton(
    modifier: GlanceModifier,
    isPlaying: Boolean,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    cornerRadius: Dp,
    iconSize: Dp = 22.sdp
) {
    val context = LocalContext.current
    val params = actionParametersOf(PlayerActions.key to PlayerActions.PLAY_PAUSE)
    WidgetIconButton(
        modifier = modifier,
        action = actionRunCallback<PlayerControlActionCallback>(params),
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        imageProvider = ImageProvider(
            if (isPlaying) R.drawable.rounded_pause_filled_24
            else R.drawable.rounded_play_arrow_filled_24
        ),
        contentDescription = context.getString(
            if (isPlaying) R.string.cd_pause else R.string.cd_play
        ),
        iconSize = iconSize,
        cornerRadius = cornerRadius
    )
}

@Composable
fun ShuffleButton(
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider,
    cornerRadius: Dp
) {
    val context = LocalContext.current
    val params = actionParametersOf(PlayerActions.key to PlayerActions.SHUFFLE)
    WidgetIconButton(
        modifier = modifier,
        action = actionRunCallback<PlayerControlActionCallback>(params),
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        imageProvider = ImageProvider(R.drawable.rounded_shuffle_24),
        contentDescription = context.getString(R.string.shortcut_shuffle_short),
        cornerRadius = cornerRadius
    )
}

@Composable
fun RepeatButton(
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    iconRes: Int,
    iconColor: ColorProvider,
    cornerRadius: Dp
) {
    val context = LocalContext.current
    val params = actionParametersOf(PlayerActions.key to PlayerActions.REPEAT)
    WidgetIconButton(
        modifier = modifier,
        action = actionRunCallback<PlayerControlActionCallback>(params),
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        imageProvider = ImageProvider(iconRes),
        contentDescription = context.getString(R.string.cd_repeat),
        cornerRadius = cornerRadius
    )
}
