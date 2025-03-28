package com.tangem.features.send.impl.navigation

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import javax.inject.Inject

@ModelScoped
internal class DefaultSendRouter @Inject constructor(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
) : InnerSendRouter {

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
                source = AppRoute.QrScanning.Source.SEND,
                networkName = network,
            ),
        )
    }
}