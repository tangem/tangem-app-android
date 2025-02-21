package com.tangem.feature.tokendetails.presentation.router

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

internal interface InnerTokenDetailsRouter {

    /** Pop back stack */
    fun popBackStack()

    /** Open website by [url] */
    fun openUrl(url: String)

    /** Share [text] */
    fun share(text: String)

    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)

    fun openStaking(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, yield: Yield)

    fun openOnrampSuccess(externalTxId: String)
}