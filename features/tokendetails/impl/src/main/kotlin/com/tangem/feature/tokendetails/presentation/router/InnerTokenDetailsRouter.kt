package com.tangem.feature.tokendetails.presentation.router

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter

internal interface InnerTokenDetailsRouter : TokenDetailsRouter {

    /** Pop back stack */
    fun popBackStack()

    /** Open website by [url] */
    fun openUrl(url: String)

    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)
}