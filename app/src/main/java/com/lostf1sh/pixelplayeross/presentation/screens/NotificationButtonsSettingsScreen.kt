package com.lostf1sh.pixelplayeross.presentation.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.lostf1sh.pixelplayeross.R
import com.lostf1sh.pixelplayeross.data.preferences.NotificationButtonAction
import com.lostf1sh.pixelplayeross.presentation.components.CollapsibleCommonTopBar
import com.lostf1sh.pixelplayeross.presentation.components.MiniPlayerHeight
import com.lostf1sh.pixelplayeross.presentation.settings.search.settingHighlight
import com.lostf1sh.pixelplayeross.presentation.viewmodel.SettingsViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Assigns the two extra buttons the media notification shows next to previous, play and next. */
@Composable
fun NotificationButtonsSettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    highlightKey: String? = null
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val notificationButtons = uiState.notificationButtons

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }

    LaunchedEffect(minTopBarHeightPx, maxTopBarHeightPx) {
        val clamped = topBarHeight.value.coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
        if (clamped != topBarHeight.value) topBarHeight.snapTo(clamped)
    }
    val collapseFraction = 1f - (
        (topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)
        ).coerceIn(0f, 1f)

    val nestedScrollConnection = remember(minTopBarHeightPx, maxTopBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val scrollingUp = available.y < 0f
                if (!scrollingUp && (
                        lazyListState.firstVisibleItemIndex > 0 ||
                            lazyListState.firstVisibleItemScrollOffset > 0
                        )
                ) return Offset.Zero

                val previous = topBarHeight.value
                val next = (previous + available.y).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = next - previous
                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(next) }
                }
                return if (scrollingUp && next == minTopBarHeightPx) Offset.Zero else Offset(0f, consumed)
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val canExpand = lazyListState.firstVisibleItemIndex == 0 &&
                lazyListState.firstVisibleItemScrollOffset == 0
            val expand = canExpand && topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2f
            topBarHeight.animateTo(
                targetValue = if (expand) maxTopBarHeightPx else minTopBarHeightPx,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    // Each action can fill only one button, so the other button's action is disabled.
    val options = linkedMapOf(
        *NotificationButtonAction.entries.map { action ->
            action.storageKey to notificationButtonLabel(action)
        }.toTypedArray()
    )

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        val currentTopBarHeight = with(density) { topBarHeight.value.toDp() }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = currentTopBarHeight + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = MiniPlayerHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {
            item(key = "notification_buttons") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ThemeSelectorItem(
                        label = stringResource(R.string.setcat_notification_button_first_title),
                        description = stringResource(R.string.setcat_notification_buttons_description),
                        options = options,
                        disabledOptionKeys = setOf(notificationButtons.second.storageKey),
                        selectedKey = notificationButtons.first.storageKey,
                        onSelectionChanged = { key ->
                            NotificationButtonAction.fromStorageKey(key)
                                ?.let(settingsViewModel::setFirstNotificationButton)
                        },
                        leadingIcon = {
                            Icon(
                                painterResource(notificationButtonIcon(notificationButtons.first)),
                                null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        modifier = Modifier.settingHighlight("item_notification_button_first", highlightKey)
                    )
                    ThemeSelectorItem(
                        label = stringResource(R.string.setcat_notification_button_second_title),
                        description = stringResource(R.string.setcat_notification_buttons_description),
                        options = options,
                        disabledOptionKeys = setOf(notificationButtons.first.storageKey),
                        selectedKey = notificationButtons.second.storageKey,
                        onSelectionChanged = { key ->
                            NotificationButtonAction.fromStorageKey(key)
                                ?.let(settingsViewModel::setSecondNotificationButton)
                        },
                        leadingIcon = {
                            Icon(
                                painterResource(notificationButtonIcon(notificationButtons.second)),
                                null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        modifier = Modifier.settingHighlight("item_notification_button_second", highlightKey)
                    )
                }
            }
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.setcat_notification_buttons_title),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeight,
            onBackClick = { navController.popBackStack() }
        )
    }
}

/** Name of [action] as shown in settings. */
@Composable
internal fun notificationButtonLabel(action: NotificationButtonAction): String = stringResource(
    when (action) {
        NotificationButtonAction.LIKE -> R.string.setcat_notification_button_like
        NotificationButtonAction.CLOSE -> R.string.setcat_notification_button_close
        NotificationButtonAction.SHUFFLE -> R.string.setcat_notification_button_shuffle
        NotificationButtonAction.REPEAT -> R.string.setcat_notification_button_repeat
    }
)

@DrawableRes
private fun notificationButtonIcon(action: NotificationButtonAction): Int = when (action) {
    NotificationButtonAction.LIKE -> R.drawable.rounded_favorite_24
    NotificationButtonAction.CLOSE -> R.drawable.rounded_close_24
    NotificationButtonAction.SHUFFLE -> R.drawable.rounded_shuffle_24
    NotificationButtonAction.REPEAT -> R.drawable.rounded_repeat_24
}
