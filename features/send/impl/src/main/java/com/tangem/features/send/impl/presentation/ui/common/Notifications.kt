package com.tangem.features.send.impl.presentation.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendNotification
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.notifications(
    notifications: ImmutableList<SendNotification>,
    modifier: Modifier = Modifier,
    hasPaddingAbove: Boolean = false,
) {
    itemsIndexed(
        items = notifications,
        key = { _, item -> item::class.java },
        contentType = { _, item -> item::class.java },
        itemContent = { i, item ->
            val bottomPadding = if (i == notifications.lastIndex) {
                TangemTheme.dimens.spacing72
            } else {
                TangemTheme.dimens.spacing0
            }
            val topPadding = if (i == 0 && hasPaddingAbove) {
                TangemTheme.dimens.spacing0
            } else {
                TangemTheme.dimens.spacing12
            }
            Notification(
                config = item.config,
                modifier = modifier
                    .padding(
                        top = topPadding,
                        bottom = bottomPadding,
                    )
                    .animateItemPlacement(),
                containerColor = when (item) {
                    is SendNotification.Error.ExceedsBalance,
                    is SendNotification.Warning.NetworkFeeUnreachable,
                    is SendNotification.Warning.HighFeeError,
                    -> TangemTheme.colors.background.action
                    else -> TangemTheme.colors.button.disabled
                },
                iconTint = when (item) {
                    is SendNotification.Error.ExceedsBalance,
                    is SendNotification.Warning,
                    -> null
                    is SendNotification.Error -> TangemTheme.colors.icon.warning
                },
            )
        },
    )
}