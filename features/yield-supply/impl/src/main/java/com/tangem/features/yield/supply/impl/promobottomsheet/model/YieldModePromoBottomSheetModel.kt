package com.tangem.features.yield.supply.impl.promobottomsheet.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics.CloseMethod
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoBottomSheetComponent
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.formatter.formatApyValue
import com.tangem.features.yield.supply.impl.promobottomsheet.entity.YieldModePromoBottomSheetUM
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class YieldModePromoBottomSheetModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analytics: AnalyticsEventHandler,
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val appCoroutineScope: AppCoroutineScope,
) : Model() {

    private val params = paramsContainer.require<YieldModePromoBottomSheetComponent.Params>()

    private var isShown = false
    private var isFinished = false

    val uiState: StateFlow<YieldModePromoBottomSheetUM>
        field = MutableStateFlow(
            YieldModePromoBottomSheetUM(
                title = resourceReference(
                    id = R.string.yield_module_main_promo_title,
                    formatArgs = wrappedList(params.offer.apy.formatApyValue()),
                ),
                tokensCount = pluralReference(
                    id = R.plurals.yield_module_main_promo_tokens_count,
                    count = params.offer.tokensCount,
                    formatArgs = wrappedList(params.offer.tokensCount),
                ),
                tokenIconUrls = TOKEN_ICON_URLS,
                onShown = ::onShown,
                onCloseClick = { close(CloseMethod.Cross) },
                onBack = { close(CloseMethod.SystemBack) },
                onDismissRequest = { close(CloseMethod.Swipe) },
                onTryClick = ::onTryClick,
            ),
        )

    fun dismiss() {
        params.onDismiss()
    }

    override fun onDestroy() {
        // The modal still plays its collapse after the slot drops it and then reports a dismissal, which isn't a close
        isFinished = true
        super.onDestroy()
    }

    private fun onShown() {
        if (isShown) return
        isShown = true

        analytics.send(YieldSupplyAnalytics.PromoScreenOpened(params.offer))
        // A quick tap on the button destroys the model, and with it a model-scoped write, before the flag lands
        appCoroutineScope.launch { yieldSupplyRepository.setYieldModePromoBottomSheetShown() }
    }

    private fun close(closeMethod: CloseMethod) {
        if (isFinished) return
        isFinished = true

        analytics.send(YieldSupplyAnalytics.PromoScreenClosed(offer = params.offer, closeMethod = closeMethod))
        params.onDismiss()
    }

    private fun onTryClick() {
        if (isFinished) return
        isFinished = true

        analytics.send(YieldSupplyAnalytics.ButtonTryYieldMode(params.offer))
        params.onDismiss()
        router.push(AppRoute.Earn(preselectedEarnType = PreselectedEarnType.Yield))
    }

    private companion object {
        // Fixed by design: the asset icons don't depend on the wallet or the offer
        val TOKEN_ICON_URLS = listOf("tether", "usd-coin", "dai")
            .map { id -> "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/$id.png" }
            .toPersistentList()
    }
}