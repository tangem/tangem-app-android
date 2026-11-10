package com.tangem.features.earn.impl

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.utils.logging.TangemLogger

/**
 * The navigator of the Yield sheet: it lives outside the Earn stack, so a route of the Earn screens opens
 * [AppRoute.EarnEntry] instead of being pushed onto a stack.
 */
internal class HostEarnNavigator(
    private val router: Router,
    private val userWalletId: UserWalletId,
    private val cryptoCurrency: CryptoCurrency,
) : EarnNavigator {

    override fun navigate(route: Route) {
        when (route) {
            is EarnInfoRoute -> openEntry(AppRoute.EarnEntry.Start.Info(productId = route.productId))
            is EarnPickerRoute -> openEntry(AppRoute.EarnEntry.Start.Picker(preselected = route.preselected))
            is EarnFlowRoute -> TangemLogger.e(
                "Earn flow can not be opened from the Yield sheet: ${route.productId.value}",
            )
            else -> router.push(route)
        }
    }

    private fun openEntry(start: AppRoute.EarnEntry.Start) {
        router.push(
            AppRoute.EarnEntry(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                source = AppRoute.EarnEntry.Source.YieldSheet,
                start = start,
            ),
        )
    }
}