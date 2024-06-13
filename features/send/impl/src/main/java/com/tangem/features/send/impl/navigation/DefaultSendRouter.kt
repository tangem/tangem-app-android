package com.tangem.features.send.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.send.impl.presentation.SendFragment

internal class DefaultSendRouter(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
) : InnerSendRouter {

    override fun getEntryFragment(): Fragment = SendFragment.create()

    override fun openUrl(url: String) {
        urlOpener.openUrl(url)
    }

    override fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency) {
        router.pop { isSuccess ->
            if (isSuccess) {
                router.push(
                    AppRoute.CurrencyDetails(
                        userWalletId = userWalletId,
                        currency = currency,
                    ),
                )
            }
        }
    }

    override fun openQrCodeScanner(network: String) {
        router.push(
            AppRoute.QrScanning(
                source = SourceType.SEND,
                networkName = network,
            ),
        )
    }
}
