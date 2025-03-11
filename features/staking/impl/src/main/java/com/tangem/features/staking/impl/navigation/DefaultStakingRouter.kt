package com.tangem.features.staking.impl.navigation

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import javax.inject.Inject

@ComponentScoped
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