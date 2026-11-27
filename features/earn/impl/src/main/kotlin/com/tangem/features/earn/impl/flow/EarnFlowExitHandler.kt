package com.tangem.features.earn.impl.flow

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.session.EarnFlowExit

internal class EarnFlowExitHandler(
    private val innerRouter: Router,
    private val hostRouter: Router,
    private val entry: EarnEntryComponent.Params,
) {

    fun onExit(productId: EarnProductId, exit: EarnFlowExit) {
        when (exit) {
            EarnFlowExit.Done -> innerRouter.pop()
            // Replaces the whole stack: a replace of the top would leave Info twice when Earn started from Info
            EarnFlowExit.ToYield -> innerRouter.replaceAll(EarnInfoRoute(productId))
            EarnFlowExit.ToToken -> openToken()
        }
    }

    private fun openToken() {
        when (entry.source) {
            EarnEntrySource.TokenPage,
            EarnEntrySource.EarnBanner,
            EarnEntrySource.YieldSheet,
            -> hostRouter.pop()
            EarnEntrySource.Markets,
            EarnEntrySource.ForYou,
            EarnEntrySource.Deeplink,
            -> hostRouter.replaceCurrent(
                AppRoute.CurrencyDetails(userWalletId = entry.userWalletId, currency = entry.cryptoCurrency),
            )
        }
    }
}