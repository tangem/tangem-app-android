package com.tangem.features.send.v2.subcomponents

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.notifications(
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