package com.tangem.feature.swap.preview

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.models.SwapSuccessStateHolder

internal data object SwapSuccessStatePreview {
    val state = SwapSuccessStateHolder(
        timestamp = 0L,
        txUrl = "https://www.google.com/#q=nam",
        fee = TextReference.Str("1 000 DAI ~ 1 000 MATIC"),
        providerName = TextReference.Str("1inch"),
        providerType = TextReference.Str(ExchangeProviderType.DEX.providerName),
        showStatusButton = false,
        providerIcon = "",
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