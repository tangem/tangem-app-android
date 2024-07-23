package com.tangem.feature.tokendetails.presentation.router

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter

internal interface InnerTokenDetailsRouter : TokenDetailsRouter {

    /** Pop back stack */
    fun popBackStack()

    /** Open website by [url] */
    fun openUrl(url: String)

    /** Share [text] */
    fun share(text: String)

    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)

    fun openStaking(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, yield: Yield)
}