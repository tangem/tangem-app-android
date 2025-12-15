package com.tangem.features.send.v2.subcomponents.destination.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.AccountTitle
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.extensions.rememberHapticFeedback
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SendAddressScreenTestTags
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.StringsSigns

/**
 * Row item with title and subtitle
 *
 * @param title title
 * @param subtitle subtitle
 * @param onClick click listener
 * @param modifier modifier
 * @param info info
 * @param subtitleEndOffset offset for subtitle ellipsis
 * @param subtitleIconRes icon
 */
@Composable
fun ListItemWithIcon(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accountTitleUM: AccountTitleUM? = null,
    info: String? = null,
    subtitleEndOffset: Int = 0,
    @DrawableRes subtitleIconRes: Int? = null,
    isLoading: Boolean = false,
) {
    AnimatedContent(
        targetState = isLoading,
        label = "Recent List Content Animation",
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
    ) { isLoadingState ->
        if (isLoadingState) {
            ListItemLoading(modifier = modifier)
        } else {
            ListItemWithIcon(
                title = title,
                subtitle = subtitle,
                onClick = onClick,
                info = info,
                accountTitleUM = accountTitleUM,
                subtitleEndOffset = subtitleEndOffset,
                subtitleIconRes = subtitleIconRes,
                modifier = modifier,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ListItemWithIcon(
    title: String,
    subtitle: String,
    accountTitleUM: AccountTitleUM?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    info: String? = null,
    subtitleEndOffset: Int = 0,
    @DrawableRes subtitleIconRes: Int? = null,
) {
    val hapticFeedback = rememberHapticFeedback(state = title, onAction = onClick)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { hapticFeedback() }
            .padding(horizontal = 12.dp)
            .testTag(SendAddressScreenTestTags.RECENT_ADDRESS_ITEM),
    ) {
        IdentIcon(
            address = title,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag(SendAddressScreenTestTags.RECENT_ADDRESS_ICON),
        )
        Column(
            modifier = Modifier
                .height(36.dp)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            EllipsisText(
                text = title,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Justify,
                ellipsis = TextEllipsis.Middle,
                modifier = Modifier.testTag(SendAddressScreenTestTags.RECENT_ADDRESS_TITLE),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (subtitleIconRes != null) {
                    Icon(
                        painter = painterResource(id = subtitleIconRes),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.informative,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(16.dp)
                            .background(TangemTheme.colors.background.tertiary, CircleShape)
                            .padding(2.dp)
                            .testTag(SendAddressScreenTestTags.RECENT_ADDRESS_TRANSACTION_ICON),
                    )
                }
                val (text, offset) = remember(subtitle, info) {
                    if (info != null) {
                        val suffix = ", $info"
                        subtitle + suffix to suffix.length + subtitleEndOffset
                    } else {
                        subtitle to 0
                    }
                }
                EllipsisText(
                    text = text,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                    ellipsis = TextEllipsis.OffsetEnd(offsetEnd = offset),
                    modifier = Modifier.testTag(SendAddressScreenTestTags.RECENT_ADDRESS_TEXT),
                )
                if (accountTitleUM != null) {
                    AccountTitle(
                        accountTitleUM = accountTitleUM,
                        textStyle = TangemTheme.typography.caption2,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListItemLoading(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        CircleShimmer(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .size(40.dp),
        )
        Column(
            modifier = Modifier
                .height(36.dp)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            RectangleShimmer(
                radius = 3.dp,
                modifier = Modifier.size(
                    width = 70.dp,
                    height = 12.dp,
                ),
            )
            RectangleShimmer(
                radius = 3.dp,
                modifier = Modifier.size(
                    width = 52.dp,
                    height = 12.dp,
                ),
            )
        }
    }
}

// region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ListItemWithIconPreview(
    @PreviewParameter(ListItemWithIconPreviewProvider::class) config: ListItemWithIconPreviewConfig,
) {
    TangemThemePreview {
        ListItemWithIcon(
            title = config.title,
            subtitle = config.subtitle,
            accountTitleUM = config.accountTitleUM,
            subtitleEndOffset = config.subtitleEndOffset,
            subtitleIconRes = config.iconRes,
            onClick = {},
            isLoading = config.isLoading,
        )
    }
}

private data class ListItemWithIconPreviewConfig(
    val title: String,
    val subtitle: String,
    val accountTitleUM: AccountTitleUM.Account? = null,
    val info: String? = null,
    val subtitleEndOffset: Int = 0,
    val iconRes: Int? = null,
    val isLoading: Boolean = false,
)

private class ListItemWithIconPreviewProvider : CollectionPreviewParameterProvider<ListItemWithIconPreviewConfig>(
    collection = listOf(
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "0.000000000000000000000000000000 BTC",
            info = "0.0.0000 at 00:00",
            subtitleEndOffset = "BTC".length,
            iconRes = R.drawable.ic_arrow_down_24,
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "1 BTC",
            info = "0.0.0000 at 00:00",
            subtitleEndOffset = "BTC".length,
            iconRes = R.drawable.ic_arrow_down_24,
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "Wallet",
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "0.000000000000000000000000000000 BTC",
            info = "0.0.0000 at 00:00",
            subtitleEndOffset = "BTC".length,
            iconRes = R.drawable.ic_arrow_down_24,
            isLoading = true,
        ),
        ListItemWithIconPreviewConfig(
            title = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            subtitle = "Wallet",
            accountTitleUM = AccountTitleUM.Account(
                name = AccountNameUM.DefaultMain.value,
                icon = CryptoPortfolioIcon.ofDefaultCustomAccount().toUM(),
                prefixText = stringReference(StringsSigns.DOT),
            ),
        ),
    ),
)
//endregion