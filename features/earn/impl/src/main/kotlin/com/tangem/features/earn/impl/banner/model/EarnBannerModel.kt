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
import com.tangem.features.earn.impl.banner.ui.state.EarnBannerUM
import com.tangem.features.earn.impl.sheet.EarnYieldSheetComponent
import com.tangem.features.earn.impl.sheet.EarnYieldSheetConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@ModelScoped
internal class EarnBannerModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    overviewSupplier: EarnOverviewSupplier,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val converter: EarnBannerConverter,
    private val router: Router,
) : Model(), EarnBannerClickIntents, EarnYieldSheetComponent.ModelCallback {

    private val params: EarnBannerComponent.Params = paramsContainer.require()

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

    override fun onBannerClick() {
        sheetNavigation.activate(EarnYieldSheetConfig)
    }

    override fun onDismiss() {
        sheetNavigation.dismiss()
    }
}