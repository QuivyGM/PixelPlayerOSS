// [Chomu] New file: the media notification's user-chosen extra buttons.
package com.lostf1sh.pixelplayeross.data.preferences

/** A custom button the media notification can show next to previous, play and next. */
enum class NotificationButtonAction(val storageKey: String) {
    LIKE("like"),
    CLOSE("close"),
    SHUFFLE("shuffle"),
    REPEAT("repeat");

    companion object {
        fun fromStorageKey(value: String?): NotificationButtonAction? =
            entries.firstOrNull { it.storageKey == value }
    }
}

/**
 * The two custom buttons the media notification shows. System media controls only have room
 * for two, so the remaining actions go to the overflow (shown on surfaces with more space,
 * such as Android Auto). The two slots always hold different actions.
 */
data class NotificationButtons(
    val first: NotificationButtonAction,
    val second: NotificationButtonAction,
) {
    init {
        require(first != second) { "Notification buttons must be different" }
    }

    /** Every action, with the two chosen ones first and the rest in their default order. */
    val orderedActions: List<NotificationButtonAction>
        get() = listOf(first, second) +
            NotificationButtonAction.entries.filter { it != first && it != second }

    companion object {
        val DEFAULT = NotificationButtons(NotificationButtonAction.LIKE, NotificationButtonAction.CLOSE)

        /**
         * Builds the slots from stored keys. A missing or unknown key falls back to its default,
         * and if both slots end up the same the second takes the first free default action.
         */
        fun fromStorageKeys(firstKey: String?, secondKey: String?): NotificationButtons {
            val first = NotificationButtonAction.fromStorageKey(firstKey) ?: DEFAULT.first
            val second = NotificationButtonAction.fromStorageKey(secondKey)
                ?.takeIf { it != first }
                ?: listOf(DEFAULT.second, DEFAULT.first).first { it != first }
            return NotificationButtons(first, second)
        }
    }
}
