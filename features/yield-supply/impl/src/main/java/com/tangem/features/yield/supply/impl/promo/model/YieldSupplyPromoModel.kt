package com.tangem.features.yield.supply.impl.promo.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.TangemSiteUrlBuilder
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.yield.supply.promo.usecase.GetBoostedApyUseCase
import com.tangem.features.yield.supply.api.YieldSupplyPromoComponent
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.YieldBoostStoryPreloader
import com.tangem.features.yield.supply.impl.promo.YieldSupplyPromoConfig
import com.tangem.features.yield.supply.impl.promo.entity.YieldSupplyPromoUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class YieldSupplyPromoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val analytics: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val appRouter: AppRouter,
    private val getBoostedApyUseCase: GetBoostedApyUseCase,
    private val boostStoryPreloader: YieldBoostStoryPreloader,
) : Model(), YieldSupplyPromoClickIntents {

    val params: YieldSupplyPromoComponent.Params = paramsContainer.require()

    val bottomSheetNavigation: SlotNavigation<YieldSupplyPromoConfig> = SlotNavigation()

    val uiState: YieldSupplyPromoUM = buildUiState()

    init {
        analytics.send(
            YieldSupplyAnalytics.EarningScreenInfoOpened(
                token = params.currency.symbol,
                blockchain = params.currency.network.name,
            ),
        )
        modelScope.launch(dispatchers.io) { boostStoryPreloader.preload() }
    }

    override fun onBackClick() {
        appRouter.pop()
    }

    override fun onApyInfoClick() {
        analytics.send(YieldSupplyAnalytics.ApyChartViewed())
        bottomSheetNavigation.activate(YieldSupplyPromoConfig.Apy)
    }

    override fun onUrlClick(url: String) {
        urlOpener.openUrl(url)
    }

    override fun onHowItWorksClick() {
        modelScope.launch {
            urlOpener.openUrl(TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.HowYieldModeWorks))
        }
    }

    override fun onStartEarningClick() {
        bottomSheetNavigation.activate(YieldSupplyPromoConfig.Action)
    }

    private fun buildUiState(): YieldSupplyPromoUM {
        val isBoost = params.isPromoEnabled
        val baseApyText = if (isBoost) "${params.apy}%" else null
        val boostedApyText = if (isBoost) {
            val baseApy = params.apy.toBigDecimalOrNull() ?: BigDecimal.ZERO
            "${getBoostedApyUseCase(baseApy)}%"
        } else {
            null
        }
        return YieldSupplyPromoUM(
            tosLink = AAVE_TOS_URL,
            policyLink = AAVE_PRIVACY_URL,
            boostTermsLink = TangemSiteUrlBuilder.YIELD_MODE_TERMS_URL,
            tokenSymbol = params.currency.symbol,
            isBoostAvailable = isBoost,
            baseApy = baseApyText,
            boostedApy = boostedApyText,
            title = resourceReference(
                R.string.yield_module_promo_screen_title_v2,
                wrappedList(params.apy),
            ),
            subtitle = resourceReference(R.string.yield_module_promo_screen_variable_rate_info_v2),
        )
    }

    private companion object {
        const val AAVE_TOS_URL = "https://aave.com/terms-of-service"
        const val AAVE_PRIVACY_URL = "https://aave.com/privacy-policy"
    }
}