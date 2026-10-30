package com.lostf1sh.pixelplayeross.ui.glancewidget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.color.colorProviders
import androidx.glance.unit.ColorProvider
import com.lostf1sh.pixelplayeross.data.model.PlayerInfo
import com.lostf1sh.pixelplayeross.data.model.WidgetThemeColors
import kotlin.math.roundToInt

/** Whether the widget is in fill mode: no background, content scaled up to the edges. */
val LocalWidgetFill = staticCompositionLocalOf { false }

/** Factor every size inside a [WidgetFrame] is multiplied by; 1 unless in fill mode. */
val LocalWidgetScale = staticCompositionLocalOf { 1f }

/** This size multiplied by the enclosing [WidgetFrame]'s scale. */
val Dp.scaled: Dp
    @Composable get() = this * LocalWidgetScale.current

/** [this] dp, scaled with the widget content. */
val Int.sdp: Dp
    @Composable get() = this.dp.scaled

/** [this] sp, scaled with the widget content. */
val Int.ssp: TextUnit
    @Composable get() = (this * LocalWidgetScale.current).sp

/**
 * Scale that grows content laid out inside [padding] on every side of [size] until it
 * reaches the nearest edges once that padding is removed. Width and height grow by the
 * same factor so sizes and spacing keep their proportions.
 */
fun fillScale(size: DpSize, padding: Dp): Float {
    val inset = padding.value * 2
    val width = size.width.value
    val height = size.height.value
    if (width <= inset || height <= inset) return 1f
    return minOf(width / (width - inset), height / (height - inset))
}

/** Background opacity the widget is drawn with; fill mode has no background at all. */
val PlayerInfo.effectiveBackgroundOpacity: Float
    get() = if (fillMode) 0f else backgroundOpacity

object AlbumArtBitmapCache {
    private const val CACHE_SIZE_BYTES = 4 * 1024 * 1024
    private val lruCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun getBitmap(key: String): Bitmap? = lruCache.get(key)

    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) {
            lruCache.put(key, bitmap)
        }
    }

    fun getKey(byteArray: ByteArray): String {
        return byteArray.contentHashCode().toString()
    }
}

data class WidgetColors(
    val surface: ColorProvider,
    val onSurface: ColorProvider,
    val artist: ColorProvider,
    val playPauseBackground: ColorProvider,
    val playPauseIcon: ColorProvider,
    val prevNextBackground: ColorProvider,
    val prevNextIcon: ColorProvider,
    /** [surface] with the user's background opacity applied; only for the widget's outer background. */
    val background: ColorProvider = surface,
    /** Plate drawn behind the artwork, title and artist. */
    val textBackdrop: TextBackdrop = TextBackdrop(surface, startOpacity = 0f, endOpacity = 0f)
)

/**
 * Plate behind the artwork and text, fading from [startOpacity] at the artwork side to
 * [endOpacity] at the far end of the text. [color] is the opaque color it is tinted with.
 */
data class TextBackdrop(
    val color: ColorProvider,
    val startOpacity: Float,
    val endOpacity: Float
) {
    val isVisible: Boolean get() = startOpacity > 0f || endOpacity > 0f
}

/** Combined coverage behind the artwork side of the text backdrop. */
const val TEXT_BACKDROP_START_COVERAGE = 0.7f

/** The title and artist never sit on a background that is more transparent than this. */
const val MIN_TEXT_BACKDROP_OPACITY = 0.3f

/**
 * Builds the backdrop for a widget whose background has [backgroundOpacity]. Layered
 * over that background, the backdrop covers [TEXT_BACKDROP_START_COVERAGE] at the
 * artwork and fades to [MIN_TEXT_BACKDROP_OPACITY] at the end of the text, so text
 * stays readable over any wallpaper. Ends already covered by the background get no plate.
 */
fun textBackdrop(color: ColorProvider, backgroundOpacity: Float) = TextBackdrop(
    color = color,
    startOpacity = textBackdropOpacity(backgroundOpacity, TEXT_BACKDROP_START_COVERAGE),
    endOpacity = textBackdropOpacity(backgroundOpacity, MIN_TEXT_BACKDROP_OPACITY)
)

/** Opacity a plate needs so that, over a background of [backgroundOpacity], the two cover [coverage]. */
private fun textBackdropOpacity(backgroundOpacity: Float, coverage: Float): Float {
    val background = backgroundOpacity.coerceIn(0f, 1f)
    if (background >= coverage) return 0f
    return 1f - (1f - coverage) / (1f - background)
}

/**
 * A white strip whose alpha runs from [startOpacity] on the left to [endOpacity] on the
 * right, meant to be stretched and tinted. Glance has no gradient brush, so widgets
 * draw gradients from a bitmap like this.
 */
fun horizontalGradientMask(startOpacity: Float, endOpacity: Float): Bitmap {
    val width = 64
    val pixels = IntArray(width) { x ->
        val fraction = x / (width - 1f)
        val alpha = startOpacity + (endOpacity - startOpacity) * fraction
        ((alpha.coerceIn(0f, 1f) * 255).roundToInt() shl 24) or 0xFFFFFF
    }
    return Bitmap.createBitmap(pixels, width, 1, Bitmap.Config.ARGB_8888)
}

/**
 * Returns this color with [opacity] applied, keeping separate day and night values so
 * the widget still follows the system theme without being recomposed.
 */
fun ColorProvider.withOpacity(context: Context, opacity: Float): ColorProvider {
    if (opacity >= 1f) return this

    fun resolve(night: Boolean): Color {
        val configuration = Configuration(context.resources.configuration).apply {
            val nightMode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
        }
        val color = getColor(context.createConfigurationContext(configuration))
        return color.copy(alpha = color.alpha * opacity.coerceIn(0f, 1f))
    }

    return ColorProvider(day = resolve(night = false), night = resolve(night = true))
}

@Composable
fun PlayerInfo.getWidgetColors(): WidgetColors {
    val theme = this.themeColors
    val context = LocalContext.current

    val colors = if (theme != null) {
        WidgetColors(
            surface = ColorProvider(
                day = Color(theme.lightSurfaceContainer),
                night = Color(theme.darkSurfaceContainer)
            ),
            onSurface = ColorProvider(
                day = Color(theme.lightTitle),
                night = Color(theme.darkTitle)
            ),
            artist = ColorProvider(
                day = Color(theme.lightArtist),
                night = Color(theme.darkArtist)
            ),
            playPauseBackground = ColorProvider(
                day = Color(theme.lightPlayPauseBackground),
                night = Color(theme.darkPlayPauseBackground)
            ),
            playPauseIcon = ColorProvider(
                day = Color(theme.lightPlayPauseIcon),
                night = Color(theme.darkPlayPauseIcon)
            ),
            prevNextBackground = ColorProvider(
                day = Color(theme.lightPrevNextBackground),
                night = Color(theme.darkPrevNextBackground)
            ),
            prevNextIcon = ColorProvider(
                day = Color(theme.lightPrevNextIcon),
                night = Color(theme.darkPrevNextIcon)
            )
        )
    } else {
        WidgetColors(
            surface = GlanceTheme.colors.surface,
            onSurface = GlanceTheme.colors.onSurface,
            artist = GlanceTheme.colors.onSurface,
            playPauseBackground = GlanceTheme.colors.primaryContainer,
            playPauseIcon = GlanceTheme.colors.onPrimaryContainer,
            prevNextBackground = GlanceTheme.colors.secondaryContainer,
            prevNextIcon = GlanceTheme.colors.onSecondaryContainer
        )
    }
    return colors.copy(
        background = colors.surface.withOpacity(context, effectiveBackgroundOpacity),
        textBackdrop = textBackdrop(colors.surface, effectiveBackgroundOpacity)
    )
}

/**
 * Glance theme roles for widgets that read [androidx.glance.GlanceTheme] directly.
 * The mapping mirrors [getWidgetColors] so every widget agrees on what the album
 * palette looks like: surface container, title/artist text, the primary play button
 * and the onPrimary skip buttons. Roles the album palette does not carry (error,
 * outline, inverse) borrow the closest of those.
 */
fun WidgetThemeColors.toGlanceColorProviders(): ColorProviders {
    fun role(light: Int, dark: Int) = ColorProvider(day = Color(light), night = Color(dark))

    val surface = role(lightSurfaceContainer, darkSurfaceContainer)
    val onSurface = role(lightTitle, darkTitle)
    val variant = role(
        lightSurfaceContainerHigh.takeUnless { it == 0 } ?: lightSurfaceContainer,
        darkSurfaceContainerHigh.takeUnless { it == 0 } ?: darkSurfaceContainer,
    )
    val onVariant = role(lightArtist, darkArtist)
    val playBackground = role(lightPlayPauseBackground, darkPlayPauseBackground)
    val playIcon = role(lightPlayPauseIcon, darkPlayPauseIcon)
    val skipBackground = role(lightPrevNextBackground, darkPrevNextBackground)
    val skipIcon = role(lightPrevNextIcon, darkPrevNextIcon)
    val error = ColorProvider(day = Color(0xFFBA1A1A), night = Color(0xFFFFB4AB))
    val onError = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF690005))

    return colorProviders(
        primary = playBackground,
        onPrimary = playIcon,
        primaryContainer = playBackground,
        onPrimaryContainer = playIcon,
        secondary = skipBackground,
        onSecondary = skipIcon,
        secondaryContainer = skipBackground,
        onSecondaryContainer = skipIcon,
        tertiary = playBackground,
        onTertiary = playIcon,
        tertiaryContainer = playBackground,
        onTertiaryContainer = playIcon,
        error = error,
        errorContainer = error,
        onError = onError,
        onErrorContainer = onError,
        background = surface,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = variant,
        onSurfaceVariant = onVariant,
        outline = onVariant,
        inverseOnSurface = surface,
        inverseSurface = onSurface,
        inversePrimary = playBackground,
    )
}
