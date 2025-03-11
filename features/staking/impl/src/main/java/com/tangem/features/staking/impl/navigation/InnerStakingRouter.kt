package com.tangem.features.staking.impl.navigation

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

internal interface InnerStakingRouter {

    fun openUrl(url: String)

    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)
}