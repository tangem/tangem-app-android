package com.tangem.features.promobanners.impl.campaigns.ui

import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.entity.ActivateCampaignUM
import com.tangem.features.promobanners.impl.campaigns.entity.FooterUM
import com.tangem.features.promobanners.impl.campaigns.entity.SelectedAccountUM
import com.tangem.features.promobanners.impl.campaigns.entity.TermsUM

/**
 * Shared preview fixtures for the campaign UI `@Preview`s. Not used in production code.
 */
internal object CampaignPreviewData {

    val tokenItem: TokenItemState.Content = TokenItemState.Content(
        id = "preview-token",
        iconState = CurrencyIconState.CoinIcon(
            url = null,
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

    val selectedAccount: SelectedAccountUM = SelectedAccountUM(
        iconState = CurrencyIconState.CryptoPortfolio.Icon(
            resId = com.tangem.core.ui.R.drawable.ic_rounded_star_24,
            color = Color(color = 0xFF0099FF),
            isGrayscale = false,
        ),
        name = stringReference("Main account"),
    )

    val footer: FooterUM = FooterUM(
        label = stringReference("Enroll"),
        onPrimaryButtonClick = {},
        terms = TermsUM(
            text = stringReference("By enrolling you agree to the"),
            linkText = stringReference("Terms & Conditions"),
            onTermsClick = {},
        ),
    )

    val activateCampaign: ActivateCampaignUM = ActivateCampaignUM(
        logo = TangemIconUM.Icon(R.drawable.ic_alert_24),
        title = stringReference("Whale Swap Cashback"),
        description = stringReference(
            "Get cashback on every swap. Pick a token and the account where your rewards will be paid out.",
        ),
        selectedToken = tokenItem,
        selectedAccount = selectedAccount,
        isChoosingToken = false,
        footerUM = footer,
        onChooseTokenDismiss = {},
        onLearnMoreClick = {},
        onChooseTokenClick = {},
    )
}