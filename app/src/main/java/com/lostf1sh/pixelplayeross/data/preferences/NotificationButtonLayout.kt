package com.lostf1sh.pixelplayeross.data.preferences

/**
 * Which pair of custom buttons the media notification shows next to previous, play and next.
 * System media controls only have room for two, so the other pair moves to the overflow
 * (shown on surfaces with more space, such as Android Auto).
 */
enum class NotificationButtonLayout(val storageKey: String) {
    /** Like and close. */
    LIKE_AND_CLOSE("like_and_close"),

    /** Shuffle and repeat. */
    SHUFFLE_AND_REPEAT("shuffle_and_repeat");

    companion object {
        val DEFAULT = LIKE_AND_CLOSE

        fun fromStorageKey(value: String?): NotificationButtonLayout =
            entries.firstOrNull { it.storageKey == value } ?: DEFAULT
    }
}
