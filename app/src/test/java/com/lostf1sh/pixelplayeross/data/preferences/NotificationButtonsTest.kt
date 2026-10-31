// [Chomu] New file: tests for the notification button slots.
package com.lostf1sh.pixelplayeross.data.preferences

import com.google.common.truth.Truth.assertThat
import com.lostf1sh.pixelplayeross.data.preferences.NotificationButtonAction.CLOSE
import com.lostf1sh.pixelplayeross.data.preferences.NotificationButtonAction.LIKE
import com.lostf1sh.pixelplayeross.data.preferences.NotificationButtonAction.REPEAT
import com.lostf1sh.pixelplayeross.data.preferences.NotificationButtonAction.SHUFFLE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NotificationButtonsTest {

    @Test
    fun `action storage keys round trip`() {
        NotificationButtonAction.entries.forEach { action ->
            assertThat(NotificationButtonAction.fromStorageKey(action.storageKey)).isEqualTo(action)
        }
    }

    @Test
    fun `defaults to like and close`() {
        assertThat(NotificationButtons.DEFAULT).isEqualTo(NotificationButtons(LIKE, CLOSE))
        assertThat(NotificationButtons.fromStorageKeys(null, null)).isEqualTo(NotificationButtons(LIKE, CLOSE))
        assertThat(NotificationButtons.fromStorageKeys("heart", "")).isEqualTo(NotificationButtons(LIKE, CLOSE))
    }

    @Test
    fun `reads stored keys`() {
        assertThat(NotificationButtons.fromStorageKeys("shuffle", "repeat"))
            .isEqualTo(NotificationButtons(SHUFFLE, REPEAT))
        assertThat(NotificationButtons.fromStorageKeys("close", "like"))
            .isEqualTo(NotificationButtons(CLOSE, LIKE))
    }

    @Test
    fun `duplicate stored keys resolve to two different actions`() {
        assertThat(NotificationButtons.fromStorageKeys("repeat", "repeat"))
            .isEqualTo(NotificationButtons(REPEAT, CLOSE))
        assertThat(NotificationButtons.fromStorageKeys("close", "close"))
            .isEqualTo(NotificationButtons(CLOSE, LIKE))
        assertThat(NotificationButtons.fromStorageKeys(null, "like"))
            .isEqualTo(NotificationButtons(LIKE, CLOSE))
    }

    @Test
    fun `the same action cannot fill both slots`() {
        assertThrows<IllegalArgumentException> { NotificationButtons(SHUFFLE, SHUFFLE) }
    }

    @Test
    fun `chosen actions come first and the rest keep their default order`() {
        assertThat(NotificationButtons(REPEAT, LIKE).orderedActions)
            .containsExactly(REPEAT, LIKE, CLOSE, SHUFFLE).inOrder()
        assertThat(NotificationButtons.DEFAULT.orderedActions)
            .containsExactly(LIKE, CLOSE, SHUFFLE, REPEAT).inOrder()
    }
}
