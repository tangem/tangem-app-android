package com.tangem.feature.tokendetails.presentation.router

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

internal interface InnerTokenDetailsRouter {

    /** Pop back stack */
    fun popBackStack()

    /** Open website by [url] */
    fun openUrl(url: String)

    /** Share [text] */
    fun share(text: String)

    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)

    fun openStaking(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, yieldId: String)
}