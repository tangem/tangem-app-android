package com.tangem.features.yield.supply.impl.promo.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.promo.YieldSupplyPromoConfig
import com.tangem.features.yield.supply.impl.promo.entity.YieldSupplyPromoUM
import com.tangem.utils.TangemBlogUrlBuilder
import com.tangem.utils.TangemBlogUrlBuilder.YIELD_SUPPLY_HOW_IT_WORKS_URL
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyPromoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val analytics: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val appRouter: AppRouter,
) : Model(), YieldSupplyPromoClickIntents {

    val params: YieldSupplyPromoComponent.Params = paramsContainer.require()

    val uiState: YieldSupplyPromoUM = YieldSupplyPromoUM(
        tosLink = TangemBlogUrlBuilder.YIELD_SUPPLY_TOS_URL,
        policyLink = TangemBlogUrlBuilder.YIELD_SUPPLY_PRIVACY_URL,
        tokenSymbol = params.currency.symbol,
        title = resourceReference(R.string.yield_module_promo_screen_title),
        subtitle = resourceReference(
            R.string.yield_module_promo_screen_variable_rate_info,
            wrappedList(params.apy),
        ),
    )

    init {
        analytics.send(
            YieldSupplyAnalytics.EarningScreenInfoOpened(
                token = params.currency.symbol,
                blockchain = params.currency.network.name,
            ),
        )
    }

    val bottomSheetNavigation: SlotNavigation<YieldSupplyPromoConfig> = SlotNavigation()

    override fun onBackClick() {
        appRouter.pop()
    }

    override fun onApyInfoClick() {
        analytics.send(YieldSupplyAnalytics.ApyChartViewed)
        bottomSheetNavigation.activate(YieldSupplyPromoConfig.Apy)
    }

    override fun onUrlClick(url: String) {
        urlOpener.openUrl(url)
    }

    override fun onHowItWorksClick() {
        urlOpener.openUrl(YIELD_SUPPLY_HOW_IT_WORKS_URL)
    }

    override fun onStartEarningClick() {
        bottomSheetNavigation.activate(YieldSupplyPromoConfig.Action)
    }
}