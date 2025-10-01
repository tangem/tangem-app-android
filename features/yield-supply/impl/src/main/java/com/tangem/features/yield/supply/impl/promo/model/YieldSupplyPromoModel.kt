package com.tangem.features.yield.supply.impl.promo.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.promo.YieldSupplyPromoConfig
import com.tangem.features.yield.supply.impl.promo.entity.YieldSupplyPromoUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyPromoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val appRouter: AppRouter,
) : Model(), YieldSupplyPromoClickIntents {

    val uiState: YieldSupplyPromoUM = YieldSupplyPromoUM(
        tosLink = "https://tangem.com/terms-of-service/", // TODO replace with real link
        policyLink = "https://tangem.com/privacy-policy/", // TODO replace with real link
        title = resourceReference(R.string.yield_module_promo_screen_title, wrappedList("5.3")),
    )

    val bottomSheetNavigation: SlotNavigation<YieldSupplyPromoConfig> = SlotNavigation()

    override fun onBackClick() {
        appRouter.pop()
    }

    override fun onApyInfoClick() {
        bottomSheetNavigation.activate(YieldSupplyPromoConfig.Apy)
    }

    override fun onUrlClick(url: String) {
        urlOpener.openUrl(url)
    }

    override fun onHowItWorksClick() {
        urlOpener.openUrl("https://tangem.com/") // TODO replace with real link
    }

    override fun onStartEarningClick() {
        bottomSheetNavigation.activate(YieldSupplyPromoConfig.Action)
    }
}