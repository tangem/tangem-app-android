package com.tangem.common.ui.notifications

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

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
                    .animateItem(),
            )
        },
    )
}