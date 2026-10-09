package com.tangem.features.tangempay.withdraw.hub.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.hub.model.WithdrawHubState
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.BankSectionUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentRecipientUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentTokenUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.TokenRecentsUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUMFactory
import kotlinx.collections.immutable.persistentListOf

private val TokenIconSize = 32.dp
private val NetworkBadgeSize = 8.dp

@Composable
internal fun TangemPayWithdrawHubScreen(state: TangemPayWithdrawHubUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        WithdrawHubTopBar(onBackClick = state.onBackClick)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SwapSection(state = state.swapSection)
            BankSection(state = state.bankSection)
        }
    }
}

@Composable
private fun WithdrawHubTopBar(onBackClick: () -> Unit) {
    TangemTopBar(
        modifier = Modifier.statusBarsPadding(),
        title = resourceReference(R.string.tangempay_card_details_withdraw),
        startContent = {
            TangemButton(
                iconStart = TangemIconUM.Icon(Icons.ic_chevron_left_20),
                onClick = onBackClick,
                size = TangemButton.Size.X11,
                variant = TangemButton.Variant.Material,
            )
        },
    )
}

@Composable
private fun SwapSection(state: TangemPayWithdrawHubUM.SwapSectionUM) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HubSectionHeader(
            title = resourceReference(R.string.tangempay_withdraw_hub_swap_section_title),
            subtitle = resourceReference(R.string.tangempay_withdraw_hub_swap_section_subtitle),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "within_portfolio") { HubActionCard(state = state.withinPortfolio) }
            when (val recents = state.recents) {
                TokenRecentsUM.Loading -> recentsShimmerItems(keyPrefix = "swap_recents_shimmer")
                TokenRecentsUM.Empty -> item(key = "swap_recents_empty") { RecentsEmptyCard() }
                is TokenRecentsUM.Content -> items(items = recents.tokens, key = { it.id }) { token ->
                    RecentTokenCard(state = token, onClick = { recents.onTokenClick(token.id) })
                }
            }
        }
    }
}

@Composable
internal fun HubSectionHeader(
    title: TextReference,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
    subtitleColor: Color = TangemTheme.colors3.text.secondary,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = subtitle.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = subtitleColor,
        )
    }
}

@Composable
private fun HubActionCard(state: TangemPayWithdrawHubUM.ActionCardUM) {
    Column(
        modifier = Modifier
            .size(width = CardWidth, height = RecentsCardHeight)
            .clip(RoundedCornerShape(RecentsCardCornerRadius))
            .background(TangemTheme.colors3.bg.status.infoSubtle)
            .clickable(onClick = state.onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        IconCircle(background = TangemTheme.colors3.bg.brand) {
            TangemIcon(modifier = Modifier.size(20.dp), tangemIconUM = state.icon)
        }
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentTokenCard(state: RecentTokenUM, onClick: () -> Unit) {
    RecentsCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            CurrencyIcon(
                state = state.icon,
                networkBadgeSize = NetworkBadgeSize,
                withFixedSize = false,
                iconSize = TokenIconSize,
                modifier = Modifier.size(TokenIconSize),
            )
            TangemBadge(
                text = resourceReference(R.string.tangempay_withdraw_hub_recent_badge),
                variant = TangemBadge.Variant.Tinted,
                status = TangemBadge.Status.Neutral,
                size = TangemBadge.Size.X4,
            )
        }
        Column {
            Text(
                text = state.name,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    text = state.accountName.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AccountIcon(
                    name = state.accountName,
                    icon = state.accountIcon,
                    size = AccountIconSize.ExtraSmall,
                )
            }
        }
    }
}

private class BankSectionPreviewProvider : PreviewParameterProvider<BankSectionUM> {
    override val values: Sequence<BankSectionUM> = sequenceOf(
        BankSectionUM.Loading,
        previewContent(recents = previewRecents(onSeeAllClick = {}), isAvailable = false),
        previewContent(recents = previewRecents(onSeeAllClick = {})),
        previewContent(recents = previewRecents(onSeeAllClick = null)),
        previewContent(recents = RecentsUM.Loading),
        previewContent(recents = RecentsUM.Empty),
        previewContent(recents = RecentsUM.Error(onRetryClick = {})),
    )
}

private fun previewRecents(onSeeAllClick: (() -> Unit)?): RecentsUM.Content = RecentsUM.Content(
    recipients = persistentListOf(
        RecentRecipientUM(
            id = "1",
            initial = "A",
            name = "Andrei M.",
            accountNumber = "0000000000",
        ),
        RecentRecipientUM(
            id = "2",
            initial = "J",
            name = "Johnny S.",
            accountNumber = "0000000000",
        ),
    ),
    onRecipientClick = {},
    onSeeAllClick = onSeeAllClick,
)

private class SwapRecentsPreviewProvider : PreviewParameterProvider<TokenRecentsUM> {
    override val values: Sequence<TokenRecentsUM> = sequenceOf(
        TokenRecentsUM.Loading,
        TokenRecentsUM.Empty,
        TokenRecentsUM.Content(
            tokens = persistentListOf(
                previewRecentToken(id = "1", name = "Polygon", accountName = "Main account"),
                previewRecentToken(
                    id = "2",
                    name = "USDC",
                    accountName = "Savings",
                    networkIconResId = R.drawable.img_polygon_22,
                ),
            ),
            onTokenClick = {},
        ),
    )
}

private fun previewContent(recents: RecentsUM, isAvailable: Boolean = true): BankSectionUM.Content =
    BankSectionUM.Content(
        toPersonalAccount = previewActionCard(
            iconRes = R.drawable.ic_user_filled_16,
            titleRes = R.string.tangempay_withdraw_hub_to_personal_account,
        ),
        toBusinessAccount = previewActionCard(
            iconRes = R.drawable.ic_briefcase_filled_16,
            titleRes = R.string.tangempay_withdraw_hub_to_business_account,
        ),
        recents = recents,
        isAvailable = isAvailable,
    )

private fun previewActionCard(iconRes: Int, titleRes: Int): TangemPayWithdrawHubUM.ActionCardUM =
    TangemPayWithdrawHubUM.ActionCardUM(
        icon = TangemIconUM.Icon(iconRes = iconRes, tintReference = { TangemTheme.colors3.icon.staticDark }),
        title = resourceReference(titleRes),
        onClick = {},
    )

private fun previewRecentToken(
    id: String,
    name: String,
    accountName: String,
    networkIconResId: Int? = null,
): RecentTokenUM = RecentTokenUM(
    id = id,
    icon = if (networkIconResId == null) {
        CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.ic_user_filled_16,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        )
    } else {
        CurrencyIconState.TokenIcon(
            url = null,
            topBadgeIconResId = networkIconResId,
            isGrayscale = false,
            shouldShowCustomBadge = false,
            fallbackTint = TangemColorPalette.White,
            fallbackBackground = TangemColorPalette.Azure,
        )
    },
    name = name,
    accountName = stringReference(accountName),
    accountIcon = AccountIconUM.CryptoPortfolio(
        value = CryptoPortfolioIcon.Icon.Letter,
        color = CryptoPortfolioIcon.Color.Azure,
    ),
)

private fun previewState(
    bankSection: BankSectionUM,
    tokenRecents: TokenRecentsUM = TokenRecentsUM.Loading,
): TangemPayWithdrawHubUM {
    val state = TangemPayWithdrawHubUMFactory(
        callbacks = TangemPayWithdrawHubUMFactory.Callbacks(
            onBackClick = {},
            onWithinPortfolioClick = {},
            onRecentTokenClick = {},
            onToPersonalAccountClick = {},
            onToBusinessAccountClick = {},
            onRecentRecipientClick = {},
            onSeeAllClick = {},
            onRetryRecentsClick = {},
        ),
    ).create(WithdrawHubState())

    return state.copy(
        bankSection = bankSection,
        swapSection = state.swapSection.copy(recents = tokenRecents),
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayWithdrawHubScreenPreview(
    @PreviewParameter(BankSectionPreviewProvider::class) bankSection: BankSectionUM,
) {
    TangemThemePreviewRedesign {
        TangemPayWithdrawHubScreen(state = previewState(bankSection))
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayWithdrawHubSwapRecentsPreview(
    @PreviewParameter(SwapRecentsPreviewProvider::class) tokenRecents: TokenRecentsUM,
) {
    TangemThemePreviewRedesign {
        TangemPayWithdrawHubScreen(
            state = previewState(bankSection = BankSectionUM.Loading, tokenRecents = tokenRecents),
        )
    }
}