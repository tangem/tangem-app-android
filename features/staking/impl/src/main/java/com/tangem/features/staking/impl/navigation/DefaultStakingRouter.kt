package com.tangem.features.staking.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.staking.impl.presentation.StakingFragment

internal class DefaultStakingRouter(
    private val urlOpener: UrlOpener,
    private val router: AppRouter,
) : InnerStakingRouter {
    override fun getEntryFragment(): Fragment = StakingFragment.create()

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