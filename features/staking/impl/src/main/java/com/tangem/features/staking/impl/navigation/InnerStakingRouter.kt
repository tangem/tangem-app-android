package com.tangem.features.staking.impl.navigation

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

internal interface InnerStakingRouter {

    fun openUrl(url: String)

    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)
}