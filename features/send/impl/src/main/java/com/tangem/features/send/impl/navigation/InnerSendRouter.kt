package com.tangem.features.send.impl.navigation

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface InnerSendRouter {

    /** Open website by [url] */
    fun openUrl(url: String)

    /** Open token details screen by [userWalletId] and [currency] */
    fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency)

    /** Open QR code scanner screen */
    fun openQrCodeScanner(network: String)
}