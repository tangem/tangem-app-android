package com.tangem.features.staking.impl.navigation

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

@ModelScoped
internal class DefaultStakingRouter @Inject constructor(
    private val urlOpener: UrlOpener,
    private val router: AppRouter,
) : InnerStakingRouter {

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
}