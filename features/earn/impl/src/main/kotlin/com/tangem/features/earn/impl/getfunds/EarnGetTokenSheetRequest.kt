package com.tangem.features.earn.impl.getfunds

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

internal data class EarnGetTokenSheetRequest(
    val userWalletId: UserWalletId,
    val cryptoCurrency: CryptoCurrency,
)