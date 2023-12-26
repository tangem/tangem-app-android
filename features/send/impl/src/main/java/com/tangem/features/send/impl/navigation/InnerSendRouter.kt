package com.tangem.features.send.impl.navigation

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.send.api.navigation.SendRouter

interface InnerSendRouter : SendRouter {

    /** Open website by [url] */
    fun openUrl(url: String)

    /** Open token details screen by [userWalletId] and [currency] */
    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)
}
