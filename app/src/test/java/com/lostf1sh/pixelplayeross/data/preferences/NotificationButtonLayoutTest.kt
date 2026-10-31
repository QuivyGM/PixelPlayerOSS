package com.lostf1sh.pixelplayeross.data.preferences

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NotificationButtonLayoutTest {

    @Test
    fun `storage keys round trip`() {
        NotificationButtonLayout.entries.forEach { layout ->
            assertThat(NotificationButtonLayout.fromStorageKey(layout.storageKey)).isEqualTo(layout)
        }
    }

    @Test
    fun `missing or unknown storage key falls back to like and close`() {
        assertThat(NotificationButtonLayout.DEFAULT).isEqualTo(NotificationButtonLayout.LIKE_AND_CLOSE)
        assertThat(NotificationButtonLayout.fromStorageKey(null)).isEqualTo(NotificationButtonLayout.LIKE_AND_CLOSE)
        assertThat(NotificationButtonLayout.fromStorageKey("")).isEqualTo(NotificationButtonLayout.LIKE_AND_CLOSE)
        assertThat(NotificationButtonLayout.fromStorageKey("heart")).isEqualTo(NotificationButtonLayout.LIKE_AND_CLOSE)
    }
}
