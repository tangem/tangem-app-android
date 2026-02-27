package com.tangem.common.ui.notifications

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

fun LazyListScope.notifications(
    notifications: ImmutableList<NotificationUM>,
    modifier: Modifier = Modifier,
    hasPaddingAbove: Boolean = false,
    isClickDisabled: Boolean = false,
) {
    itemsIndexed(
        items = notifications,
        key = { _, item -> item::class.java },
        contentType = { _, item -> item::class.java },
        itemContent = { i, item ->
            val topPadding = if (i == 0 && hasPaddingAbove) 0.dp else 12.dp
            Notification(
                config = item.config,
                modifier = modifier
                    .padding(top = topPadding)
                    .animateItem(),
                containerColor = when (item) {
                    is NotificationUM.Error.TokenExceedsBalance,
                    is NotificationUM.Warning.NetworkFeeUnreachable,
                    is NotificationUM.Warning.HighFeeError,
                    -> TangemTheme.colors.background.action
                    else -> TangemTheme.colors.button.disabled
                },
                iconTint = when (item) {
                    is NotificationUM.Error.TokenExceedsBalance,
                    is NotificationUM.Warning,
                    -> null
                    is NotificationUM.Error -> TangemTheme.colors.icon.warning
                    is NotificationUM.Info -> TangemTheme.colors.icon.accent
                },
                isEnabled = !isClickDisabled,
            )
        },
    )
}

/**
 * Displays a list of notifications using TangemMessage composables.
 *
 * @param notifications List of NotificationConfig objects to be displayed.
 * @param contentColor Color to be used for the content of the notifications.
 * @param modifier Optional Modifier for the notifications.
 * @param hasPaddingAbove Boolean indicating whether to add padding above the first notification.
 */
fun LazyListScope.notifications2(
    notifications: ImmutableList<NotificationConfig>,
    contentColor: Color,
    modifier: Modifier = Modifier,
    hasPaddingAbove: Boolean = false,
) {
    itemsIndexed(
        items = notifications,
        key = { index, item -> item.title?.hashCode()?.plus(index) ?: index },
        contentType = { _, item -> item::class.java },
        itemContent = { i, item ->
            val topPadding = if (i == 0 && hasPaddingAbove) 0.dp else 12.dp
            TangemMessage(
                config = item,
                contentColor = contentColor,
                modifier = modifier
                    .padding(top = topPadding)
                    .animateItem(),
            )
        },
    )
}

/**
 * Displays a list of notifications using TangemMessage composables.
 *
 * @param notifications List of TangemMessageUM objects to be displayed.
 * @param contentColor Color to be used for the content of the notifications.
 * @param modifier Optional Modifier for the notifications.
 * @param hasPaddingAbove Boolean indicating whether to add padding above the first notification.
 */
fun LazyListScope.notifications(
    notifications: ImmutableList<TangemMessageUM>,
    contentColor: Color,
    modifier: Modifier = Modifier,
    hasPaddingAbove: Boolean = false,
) {
    itemsIndexed(
        items = notifications,
        key = { _, item -> item.id },
        contentType = { _, item -> item::class.java },
        itemContent = { i, item ->
            val topPadding = if (i == 0 && hasPaddingAbove) TangemTheme.dimens2.x0 else TangemTheme.dimens2.x2
            TangemMessage(
                messageUM = item,
                contentColor = contentColor,
                modifier = modifier
                    .padding(top = topPadding)
                    .animateItem(null, null, null),
            )
        },
    )
}

/**
 * Displays a list of notifications in a stacked manner using a HorizontalPager.
 * If there are multiple notifications, a PagerIndicator is shown below the notifications.
 *
 * @param notifications     List of TangemMessageUM objects to be displayed.
 * @param containerColor    Color to be used for the background of the notifications.
 * @param modifier          Optional Modifier for the notifications.
 */
fun LazyListScope.notificationsCarousel(
    notifications: ImmutableList<TangemMessageUM>?,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    item {
        if (!notifications.isNullOrEmpty()) {
            val notificationsPagerState = rememberPagerState(
                pageCount = { notifications.size },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = TangemTheme.dimens2.x2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
            ) {
                HorizontalPager(
                    state = notificationsPagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .animateItem(null, null, null),
                ) { page ->
                    TangemMessage(
                        messageUM = notifications[page],
                        contentColor = containerColor,
                        modifier = modifier,
                    )
                }
                if (notifications.size > 1) {
                    TangemPagerIndicator(
                        pagerState = notificationsPagerState,
                    )
                }
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun StackedNotifications_Preview(
    @PreviewParameter(StackedNotificationsPreviewProvider::class) params: ImmutableList<TangemMessageUM>,
) {
    TangemThemePreviewRedesign {
        val contentColor = TangemTheme.colors2.surface.level1
        LazyColumn(
            modifier = Modifier
                .background(contentColor)
                .padding(16.dp),
        ) {
            notificationsCarousel(
                notifications = params,
                containerColor = contentColor,
            )
        }
    }
}

private class StackedNotificationsPreviewProvider : PreviewParameterProvider<ImmutableList<TangemMessageUM>> {
    override val values: Sequence<ImmutableList<TangemMessageUM>>
        get() = sequenceOf(
            persistentListOf(
                TangemMessageUM(
                    id = "0",
                    title = stringReference("First notification"),
                    subtitle = stringReference("This is the first notification"),
                    messageEffect = TangemMessageEffect.Magic,
                ),
            ),
            persistentListOf(
                TangemMessageUM(
                    id = "0",
                    title = stringReference("First notification"),
                    subtitle = stringReference("This is the first notification"),
                    messageEffect = TangemMessageEffect.Magic,
                ),
                TangemMessageUM(
                    id = "1",
                    title = stringReference("Second notification"),
                    subtitle = stringReference("This is the second notification"),
                    messageEffect = TangemMessageEffect.Card,
                ),
            ),
        )
}
// endregion