package com.tangem.feature.tokendetails.presentation.router

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import javax.inject.Inject

@ModelScoped
internal class DefaultTokenDetailsRouter @Inject constructor(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
) : InnerTokenDetailsRouter {

    override fun popBackStack() {
        router.pop()
    }

    override fun openUrl(url: String) {
        urlOpener.openUrl(url)
    }

    override fun share(text: String) {
        shareManager.shareText(text)
    }

    override fun openTokenDetails(userWalletId: UserWalletId, currency: CryptoCurrency) {
        router.push(
            AppRoute.CurrencyDetails(
                userWalletId = userWalletId,
                currency = currency,
            ),
        )
    }

    override fun openStaking(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, yieldId: String) {
        router.push(
            AppRoute.Staking(
                userWalletId = userWalletId,
                cryptoCurrencyId = cryptoCurrency.id,
                yieldId = yieldId,
            ),
        )
    }
}