package com.tangem.features.foryou.impl.entity

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

internal sealed interface ForYouBottomSheetConfig {

    data object AddToPortfolio : ForYouBottomSheetConfig

    data class ManageFunds(
        val rawCurrencyId: CryptoCurrency.RawID,
    ) : ForYouBottomSheetConfig

    data class AddFunds(
        val userWalletId: UserWalletId,
    ) : ForYouBottomSheetConfig
}