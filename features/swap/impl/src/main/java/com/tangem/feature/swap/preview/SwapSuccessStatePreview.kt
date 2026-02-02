package com.tangem.feature.swap.preview

import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.models.SwapSuccessStateHolder

internal data object SwapSuccessStatePreview {
    val state = SwapSuccessStateHolder(
        timestamp = 0L,
        txUrl = "https://www.google.com/#q=nam",
        fee = TextReference.Str("1 000 DAI ~ 1 000 MATIC"),
        providerName = TextReference.Str("1inch"),
        providerType = TextReference.Str(ExchangeProviderType.DEX.providerName),
        shouldShowStatusButton = false,
        providerIcon = "",
        fromTitle = AccountTitleUM.Account(
            prefixText = stringReference("From"),
            name = AccountNameUM.DefaultMain.value,
            icon = CryptoPortfolioIconConverter.convert(CryptoPortfolioIcon.ofDefaultCustomAccount()),
        ),
        toTitle = AccountTitleUM.Account(
            prefixText = stringReference("To"),
            name = AccountNameUM.DefaultMain.value,
            icon = CryptoPortfolioIconConverter.convert(CryptoPortfolioIcon.ofDefaultCustomAccount()),
        ),
        fromTokenAmount = TextReference.Str("1 000 DAI"),
        toTokenAmount = TextReference.Str("1 000 MATIC"),
        fromTokenFiatAmount = TextReference.Str("1 000 $"),
        toTokenFiatAmount = TextReference.Str("1 000 $"),
        fromTokenIconState = CurrencyIconState.Loading,
        toTokenIconState = CurrencyIconState.Loading,
        rate = TextReference.Str("1 000 DAI ~ 1 000 MATIC"),
        onExploreButtonClick = {},
        onStatusButtonClick = {},
    )
}