package com.tangem.features.earn.impl.getfunds

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.impl.analytics.EarnAddFundsSource

internal data class EarnGetTokenSheetRequest(
    val userWalletId: UserWalletId,
    val cryptoCurrency: CryptoCurrency,
    val source: EarnAddFundsSource,
)