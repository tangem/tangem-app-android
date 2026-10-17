package com.tangem.features.promobanners.impl.campaigns.v2.ui

import androidx.compose.ui.graphics.Color
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoBlockUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignFooterUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignStatusUM
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
        link = PromoLinkUM(text = stringReference("Learn more"), onClick = {}),
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

    private val enrolled = PromoCampaignStatusUM(
        variant = TangemStatusScreen.Variant.Success,
        icon = TangemStatusIcon.Kind.Checkmark,
        title = stringReference("You’re successfully enrolled in Whale Swap Cashback"),
        description = null,
        details = null,
        buttonText = stringReference("Got it"),
        onButtonClick = {},
    )

    private val alreadyEnrolled = PromoCampaignStatusUM(
        variant = TangemStatusScreen.Variant.Info,
        icon = TangemStatusIcon.Kind.Info,
        title = stringReference("You’re already enrolled in Whale Swap Cashback"),
        description = null,
        details = null,
        buttonText = stringReference("Close"),
        onButtonClick = {},
    )

    private val unavailable = PromoCampaignStatusUM(
        variant = TangemStatusScreen.Variant.Warning,
        icon = TangemStatusIcon.Kind.Warning,
        title = stringReference("Campaign unavailable"),
        description = stringReference("This campaign no longer exists or has expired"),
        details = null,
        buttonText = stringReference("Close"),
        onButtonClick = {},
    )

    val statuses = listOf(
        enrolled,
        enrolled.copy(description = stringReference("All of your wallets are enrolled")),
        enrolled.copy(
            description = stringReference("Some of your wallets are enrolled"),
            details = PromoCampaignStatusUM.Details.EnrolledWallets(
                wallets = persistentListOf(
                    wallet(id = "1", name = "Main wallet", info = "3 cards", balance = "$4,496.75"),
                    wallet(id = "2", name = "Multi Card", info = "1 card", balance = "$0.03"),
                ),
            ),
        ),
        alreadyEnrolled.copy(
            details = PromoCampaignStatusUM.Details.Recipient(
                walletName = stringReference("Tangem wallet 2.0"),
                tokenIcon = tokenItem.iconState,
                address = "33BddS...ga2B",
            ),
        ),
        alreadyEnrolled,
        unavailable,
        unavailable.copy(
            title = stringReference("Campaign not available yet"),
            description = stringReference("Please try again later."),
        ),
    )

    private fun wallet(id: String, name: String, info: String, balance: String) = UserWalletItemUM(
        id = id,
        name = stringReference(name),
        information = UserWalletItemUM.Information.Loaded(stringReference(info)),
        balance = UserWalletItemUM.Balance.Loaded(value = balance, isFlickering = false),
        imageState = UserWalletItemUM.ImageState.MobileWallet,
        isEnabled = true,
        onClick = {},
    )

    private fun tile(label: String, value: String) = PromoBlockUM.Tile(
        label = stringReference(label),
        value = stringReference(value),
    )
}