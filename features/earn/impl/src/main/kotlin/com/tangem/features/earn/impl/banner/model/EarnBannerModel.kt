package com.tangem.features.earn.impl.banner.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.EarnBannerComponent
import com.tangem.features.earn.api.overview.EarnOverviewSupplier
import com.tangem.features.earn.impl.HostEarnNavigator
import com.tangem.features.earn.impl.banner.ui.state.EarnBannerUM
import com.tangem.features.earn.impl.overview.EarnProductSummaries
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.sheet.EarnYieldSheetComponent
import com.tangem.features.earn.impl.sheet.EarnYieldSheetConfig
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class EarnBannerModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    overviewSupplier: EarnOverviewSupplier,
    private val summarySupplier: EarnProductSummarySupplier,
    private val registry: EarnProductsRegistry,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val converter: EarnBannerConverter,
    private val router: Router,
) : Model(), EarnBannerClickIntents, EarnYieldSheetComponent.ModelCallback {

    private val params: EarnBannerComponent.Params = paramsContainer.require()

    private val productParams = EarnProduct.Params(params.userWalletId, params.cryptoCurrency)

    private val navigator = HostEarnNavigator(
        router = router,
        userWalletId = params.userWalletId,
        cryptoCurrency = params.cryptoCurrency,
        source = AppRoute.EarnEntry.Source.EarnBanner,
    )

    val sheetNavigation = SlotNavigation<EarnYieldSheetConfig>()

    val uiState: StateFlow<EarnBannerUM>
        field = MutableStateFlow<EarnBannerUM>(EarnBannerUM.Hidden)

    init {
        combine(
            flow = overviewSupplier(params.userWalletId),
            flow2 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        ) { overviews, isBalanceHidden ->
            converter.convert(
                overview = overviews[params.cryptoCurrency.id],
                currency = params.cryptoCurrency,
                isBalanceHidden = isBalanceHidden,
                intents = this,
            )
        }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    override fun onEnableClick(preselected: EarnProductId?) {
        router.push(
            AppRoute.EarnEntry(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.cryptoCurrency,
                source = AppRoute.EarnEntry.Source.EarnBanner,
                start = AppRoute.EarnEntry.Start.Picker(preselected = preselected),
            ),
        )
    }

    // PRD 3.1, callout 5: a sheet with a single row is skipped, the banner does what the row would do
    override fun onBannerClick() {
        modelScope.launch {
            val single = summarySupplier(params.userWalletId).first().singleProduct()
            if (single == null) {
                sheetNavigation.activate(EarnYieldSheetConfig)
            } else {
                val (product, summary) = single
                product.actionHandler(productParams, navigator).onActionClick(summary.onClickAction)
            }
        }
    }

    override fun onDismiss() {
        sheetNavigation.dismiss()
    }

    /** `null` while products are loading: the sheet may still get more rows. */
    private fun EarnProductSummaries.singleProduct(): Pair<EarnProduct, EarnProductSummary>? {
        val currencyId = params.cryptoCurrency.id
        if (isPending(currencyId)) return null

        return this[currencyId]
            .mapNotNull { summary -> registry.find(summary.productId)?.let { product -> product to summary } }
            .singleOrNull()
    }
}