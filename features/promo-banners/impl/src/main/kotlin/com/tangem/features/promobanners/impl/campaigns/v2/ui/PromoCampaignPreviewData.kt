package com.tangem.features.promobanners.impl.campaigns.v2.ui

import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoBlockUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignFooterUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoLinkUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoSelectedAccountUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoTokenSelectorUM
import kotlinx.collections.immutable.persistentListOf

internal object PromoCampaignPreviewData {

    val oddTiles = PromoBlockUM.Tiles(
        title = stringReference("Cashback and amounts"),
        tiles = persistentListOf(
            tile("From \$10K", "0.10%"),
            tile("From \$20K", "0.20%"),
            tile("From \$100K", "0.50%"),
        ),
    )

    val evenTiles = PromoBlockUM.Tiles(
        title = null,
        tiles = persistentListOf(
            tile("From \$10K", "0.10%"),
            tile("From \$20K", "0.20%"),
            tile("From \$50K", "0.30%"),
            tile("From \$100K", "0.50%"),
        ),
    )

    val table = PromoBlockUM.Table(
        title = stringReference("Max payout and limits"),
        headers = persistentListOf(),
        rows = persistentListOf(
            persistentListOf(stringReference("Per swap"), stringReference("\$500")),
            persistentListOf(stringReference("Per wallet per swap"), stringReference("\$10K")),
        ),
    )

    val tableWithHeaders = table.copy(
        headers = persistentListOf(stringReference("Option"), stringReference("Amount")),
    )

    val text = PromoBlockUM.Text(
        title = stringReference("Max payout and limits"),
        text = stringReference(
            "Stable coin into stablecoin swaps are excluded. " +
                "Payout arrives weekly in USDT or USDC address of your choice.",
        ),
        learnMore = PromoLinkUM(text = stringReference("Learn more"), onClick = {}),
    )

    val highlight = PromoBlockUM.Highlight(
        title = stringReference("You have \$30 in rewards"),
        description = stringReference("This total includes rewards from all incentivised tokens"),
    )

    val highlightTitleOnly = highlight.copy(description = null)

    val shortBlocks = persistentListOf(oddTiles, table)

    val campaign = PromoCampaignUM(
        imageUrl = "",
        title = stringReference("Enroll in Whale Swap Cashback"),
        subtitle = stringReference("Earn cashback on every swap from \$10K until the end of July"),
        blocks = persistentListOf(oddTiles, table, text, highlight),
        tokenSelector = PromoTokenSelectorUM(selectedToken = null, selectedAccount = null, onClick = {}),
        // No payout token chosen yet: the action is choosing it, so there are no terms.
        footer = PromoCampaignFooterUM(
            terms = null,
            buttonText = stringReference("Select cashback token"),
            onButtonClick = {},
        ),
    )

    private val tokenItem = TokenItemState.Content(
        id = "preview-token",
        iconState = CurrencyIconState.CoinIcon(
            url = "[REDACTED_ENV_URL]",
            fallbackResId = com.tangem.core.ui.R.drawable.img_polygon_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
        titleState = TokenItemState.TitleState.Content(text = stringReference("Polygon")),
        subtitleState = TokenItemState.SubtitleState.TextContent(value = stringReference("MATIC")),
        fiatAmountState = TokenItemState.FiatAmountState.Content(text = "321 $"),
        subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "5,412 MATIC"),
        onItemClick = {},
        onItemLongClick = {},
    )

    private val selectedAccount = PromoSelectedAccountUM(
        iconState = CurrencyIconState.CryptoPortfolio.Icon(
            resId = com.tangem.core.ui.R.drawable.ic_rounded_star_24,
            color = Color(color = 0xFF0099FF),
            isGrayscale = false,
        ),
        name = stringReference("Main account"),
    )

    val campaignWithSelectedToken = campaign.copy(
        blocks = persistentListOf(),
        tokenSelector = PromoTokenSelectorUM(
            selectedToken = tokenItem,
            selectedAccount = selectedAccount,
            onClick = {},
        ),
        footer = PromoCampaignFooterUM(
            terms = PromoLinkUM(text = stringReference("Terms & Conditions"), onClick = {}),
            buttonText = stringReference("Enroll"),
            onButtonClick = {},
        ),
    )

    private fun tile(label: String, value: String) = PromoBlockUM.Tile(
        label = stringReference(label),
        value = stringReference(value),
    )
}