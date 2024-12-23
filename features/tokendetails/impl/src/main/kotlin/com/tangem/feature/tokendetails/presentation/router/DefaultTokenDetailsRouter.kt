package com.tangem.feature.tokendetails.presentation.router

import androidx.fragment.app.Fragment
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.tokendetails.presentation.TokenDetailsFragment

internal class DefaultTokenDetailsRouter(
    private val router: AppRouter,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
) : InnerTokenDetailsRouter {

    override fun getEntryFragment(): Fragment = TokenDetailsFragment()

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

    override fun openStaking(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, yield: Yield) {
        router.push(
            AppRoute.Staking(
                userWalletId = userWalletId,
                cryptoCurrencyId = cryptoCurrency.id,
                yield = yield,
            ),
        )
    }

    override fun openOnrampSuccess(externalTxId: String) {
        // finish current onramp flow and show onramp success screen
        val replaceOnrampScreens = router.stack
            .filterNot { it is AppRoute.Onramp }
            .toMutableList()

        replaceOnrampScreens.add(AppRoute.OnrampSuccess(externalTxId))

        router.replaceAll(*replaceOnrampScreens.toTypedArray())
    }
}