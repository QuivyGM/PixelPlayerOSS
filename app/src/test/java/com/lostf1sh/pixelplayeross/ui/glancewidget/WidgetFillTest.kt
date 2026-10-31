// [Chomu] New file: tests for the widget fill mode math.
package com.lostf1sh.pixelplayeross.ui.glancewidget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class WidgetFillTest {

    @Test
    fun `fill is only allowed at or above the minimum size`() {
        assertThat(canFill(DpSize(120.dp, 100.dp))).isTrue()
        assertThat(canFill(DpSize(300.dp, 200.dp))).isTrue()
        assertThat(canFill(DpSize(119.dp, 200.dp))).isFalse()
        assertThat(canFill(DpSize(300.dp, 99.dp))).isFalse()
    }

    @Test
    fun `fill scale is capped`() {
        // Growing into all of the 16 dp padding would be about 1.15 here.
        assertThat(fillScale(DpSize(250.dp, 250.dp), padding = 16.dp, inset = 0.dp))
            .isEqualTo(MAX_FILL_SCALE)
    }

    @Test
    fun `fill scale stops at the inset`() {
        // 200 dp with 16 dp padding has 168 dp of content; a 12 dp inset leaves room for 176.
        assertThat(fillScale(DpSize(200.dp, 200.dp), padding = 16.dp, inset = 12.dp))
            .isWithin(1e-4f).of(176f / 168f)
    }

    @Test
    fun `fill scale never shrinks content`() {
        assertThat(fillScale(DpSize(200.dp, 200.dp), padding = 16.dp, inset = 20.dp)).isEqualTo(1f)
        assertThat(fillScale(DpSize(20.dp, 20.dp), padding = 16.dp, inset = 0.dp)).isEqualTo(1f)
    }

    @Test
    fun `fill padding leaves exactly the scaled content`() {
        assertThat(fillPadding(200.dp, padding = 16.dp, scale = 1f)).isEqualTo(16.dp)

        val scale = 1.1f
        val padding = fillPadding(200.dp, padding = 16.dp, scale = scale)
        assertThat((200.dp - padding * 2).value).isWithin(1e-3f).of(168f * scale)
    }
}
