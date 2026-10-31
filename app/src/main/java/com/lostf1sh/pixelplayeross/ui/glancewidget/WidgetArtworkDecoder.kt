package com.lostf1sh.pixelplayeross.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.lostf1sh.pixelplayeross.utils.AlbumArtUtils
import com.lostf1sh.pixelplayeross.utils.ArtworkTransportSanitizer
import com.lostf1sh.pixelplayeross.utils.LocalArtworkUri
import timber.log.Timber // [Chomu]
import java.io.IOException // [Chomu]

private val SUPPORTED_WIDGET_LOCAL_ARTWORK_SCHEMES = setOf(
    "content",
    "file",
    "android.resource",
    LocalArtworkUri.SCHEME,
)

internal fun decodeWidgetAlbumArtBitmap(
    context: Context,
    rawUri: String,
    targetWidthPx: Int,
    targetHeightPx: Int,
): Bitmap? {
    val isLocalArtworkUri = LocalArtworkUri.isLocalArtworkUri(rawUri)
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    val isSupportedLocalUri = isLocalArtworkUri ||
        scheme in SUPPORTED_WIDGET_LOCAL_ARTWORK_SCHEMES ||
        (scheme.isNullOrBlank() && rawUri.startsWith("/"))
    if (!isSupportedLocalUri) {
        return null
    }

    // region [Chomu] Fall back to the placeholder when the artwork can't be read
    // Songs without artwork make the artwork provider throw FileNotFoundException; that and
    // any other read failure means the widget shows the placeholder instead of failing.
    val appContext = context.applicationContext
    val encodedBytes = try {
        AlbumArtUtils.openArtworkInputStream(appContext, uri)?.use { input ->
            readBytesCapped(input, ArtworkTransportSanitizer.WIDGET_CONFIG.sourceBytesLimit)
        }
    } catch (e: IOException) {
        Timber.tag("WidgetArtworkDecoder").d("No readable artwork for %s: %s", rawUri, e.message)
        null
    } catch (e: SecurityException) {
        Timber.tag("WidgetArtworkDecoder").w(e, "Artwork not readable for %s", rawUri)
        null
    } ?: return null
    // endregion [Chomu]

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(encodedBytes, 0, encodedBytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    // region [Chomu] A target below 1 px would never stop the halving below.
    if (targetWidthPx <= 0 || targetHeightPx <= 0) {
        return null
    }
    // endregion [Chomu]

    var inSampleSize = 1
    if (bounds.outHeight > targetHeightPx || bounds.outWidth > targetWidthPx) {
        val halfHeight = bounds.outHeight / 2
        val halfWidth = bounds.outWidth / 2
        while (
            halfHeight / inSampleSize >= targetHeightPx &&
            halfWidth / inSampleSize >= targetWidthPx
        ) {
            inSampleSize *= 2
        }
    }

    return BitmapFactory.decodeByteArray(
        encodedBytes,
        0,
        encodedBytes.size,
        BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inJustDecodeBounds = false
        }
    )
}

private fun readBytesCapped(input: java.io.InputStream, maxBytes: Int): ByteArray? {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalRead = 0
    while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        totalRead += read
        if (totalRead > maxBytes) return null
        output.write(buffer, 0, read)
    }
    return output.toByteArray().takeIf { it.isNotEmpty() }
}
