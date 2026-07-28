package com.tangem.common.ui.notifications

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.message.TangemMessageButtonUM
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.NotificationTestTags
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
 * Displays a list of notifications using the DS3 [TangemMessageBanner].
 *
 * @param notifications List of TangemMessageUM objects to be displayed.
 * @param modifier Optional Modifier for the notifications.
 * @param hasPaddingAbove Boolean indicating whether to add padding above the first notification.
 */
fun LazyListScope.notifications(
    notifications: ImmutableList<TangemMessageUM>,
    modifier: Modifier = Modifier,
    hasPaddingAbove: Boolean = false,
) {
    itemsIndexed(
        items = notifications,
        key = { _, item -> item.id },
        contentType = { _, item -> item::class.java },
        itemContent = { i, item ->
            val topPadding = if (i == 0 && hasPaddingAbove) TangemTheme.dimens2.x0 else TangemTheme.dimens2.x2
            MessageBanner(
                messageUM = item,
                modifier = modifier
                    .padding(top = topPadding)
                    .animateItem(null, null, null),
            )
        },
    )
}

/**
 * Renders a [TangemMessageUM] with the DS3 [TangemMessageBanner], decomposing the message model into the
 * banner's parameters: title/subtitle become the header, [TangemMessageUM.messageEffect] selects the
 * [TangemMessageBanner.Variant], the icon fills `slotStart`, the close handler maps to the [CloseButton]
 * preset in `slotEnd`, and each button maps to the primary/secondary slot by its [TangemButtonType].
 */
@Composable
private fun MessageBanner(messageUM: TangemMessageUM, modifier: Modifier = Modifier) {
    val iconUM = messageUM.iconUM
    TangemMessageBanner(
        modifier = modifier
            .testTag(NotificationTestTags.CONTAINER)
            .conditional(messageUM.onClick != null) {
                clickableSingle(onClick = requireNotNull(messageUM.onClick))
            },
        title = messageUM.title,
        description = messageUM.subtitle,
        variant = messageUM.messageEffect.toBannerVariant(),
        showGlowRing = messageUM.messageEffect.hasGlowRing(),
        contentAlign = if (messageUM.isCentered) {
            TangemMessageBanner.ContentAlign.Center
        } else {
            TangemMessageBanner.ContentAlign.Start
        },
        secondaryButton = messageUM.buttonsUM
            .firstOrNull { it.type == TangemButtonType.Secondary }
            ?.toBannerButton(),
        primaryButton = messageUM.buttonsUM
            .firstOrNull { it.type != TangemButtonType.Secondary }
            ?.toBannerButton(),
        slotStart = iconUM?.let {
            {
                TangemIcon(
                    tangemIconUM = it,
                    modifier = Modifier
                        .size(messageUM.iconSize)
                        .testTag(NotificationTestTags.ICON),
                )
            }
        },
        slotEnd = messageUM.onCloseClick?.let { onCloseClick ->
            { TangemMessageBanner.CloseButton(onClick = onCloseClick) }
        },
    )
}

/**
 * Maps the message [TangemMessageEffect] to the DS3 banner [TangemMessageBanner.Variant] (background):
 * the legacy [Warning][TangemMessageEffect.Warning] alert becomes the red [Error][TangemMessageBanner.Variant.Error],
 * everything else keeps the neutral [Solid][TangemMessageBanner.Variant.Solid] background.
 */
private fun TangemMessageEffect.toBannerVariant(): TangemMessageBanner.Variant = when (this) {
    TangemMessageEffect.Warning -> TangemMessageBanner.Variant.Error
    TangemMessageEffect.Magic,
    TangemMessageEffect.Card,
    TangemMessageEffect.None,
    -> TangemMessageBanner.Variant.Solid
}

/**
 * Whether the DS3 banner shows a glow ring for this effect. The ring color follows the
 * [variant][toBannerVariant] — the multi-color [Magic][TangemGlowRing.Variant.Magic] ring for the
 * [Solid][TangemMessageBanner.Variant.Solid] vibrant effects and the red ring for the
 * [Error][TangemMessageBanner.Variant.Error] alert; [None][TangemMessageEffect.None] stays flat.
 */
private fun TangemMessageEffect.hasGlowRing(): Boolean = when (this) {
    TangemMessageEffect.Warning,
    TangemMessageEffect.Magic,
    TangemMessageEffect.Card,
    -> true
    TangemMessageEffect.None -> false
}

/** Maps a [TangemMessageButtonUM] to a [TangemMessageBanner.Button], keeping its trailing icon. */
private fun TangemMessageButtonUM.toBannerButton(): TangemMessageBanner.Button = TangemMessageBanner.Button(
    text = text,
    onClick = onClick,
    iconEnd = tangemIconUM,
    isLoading = isLoading,
)

/**
 * Displays a list of notifications in a stacked manner using a HorizontalPager.
 * If there are multiple notifications, a PagerIndicator is shown below the notifications.
 *
 * @param notifications     List of TangemMessageUM objects to be displayed.
 * @param modifier          Optional Modifier for the notifications.
 */
fun LazyListScope.notificationsCarousel(
    notifications: ImmutableList<TangemMessageUM>?,
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
                    MessageBanner(
                        messageUM = notifications[page],
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