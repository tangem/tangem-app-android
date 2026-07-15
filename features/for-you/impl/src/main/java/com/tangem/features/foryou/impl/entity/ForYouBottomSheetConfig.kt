package com.tangem.features.foryou.impl.entity

import com.tangem.domain.models.currency.CryptoCurrency

internal sealed interface ForYouBottomSheetConfig {

    data object AddToPortfolio : ForYouBottomSheetConfig

    data class ManageFunds(
        val rawCurrencyId: CryptoCurrency.RawID,
    ) : ForYouBottomSheetConfig
}