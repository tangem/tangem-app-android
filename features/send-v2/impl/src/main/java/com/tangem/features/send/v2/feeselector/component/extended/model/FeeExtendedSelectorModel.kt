package com.tangem.features.send.v2.feeselector.component.extended.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.feeselector.component.FeeSelectorComponentParams
import com.tangem.features.send.v2.feeselector.component.extended.entity.FeeExtendedSelectorUM
import com.tangem.features.send.v2.feeselector.route.FeeSelectorRoute
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
class FeeExtendedSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val router: Router,
) : Model() {

    private val params = paramsContainer.require<FeeSelectorComponentParams>()
    private var appCurrency: AppCurrency = AppCurrency.Default
    private val tokenConverter = SelectedTokenItemConverter(
        appCurrencyProvider = Provider { appCurrency },
        onTokenClick = { router.push(FeeSelectorRoute.ChooseToken) },
    )

    init {
        initAppCurrency()
    }

    val uiState: StateFlow<FeeExtendedSelectorUM>
        field = MutableStateFlow<FeeExtendedSelectorUM>(getInitialState())

    init {
        params.state
            .filterIsInstance<FeeSelectorUM.Content>()
            .onEach(::onParentStateUpdated)
            .launchIn(modelScope)
    }

    private fun getInitialState(): FeeExtendedSelectorUM {
        val parentContentState = params.state.value as FeeSelectorUM.Content

        return FeeExtendedSelectorUM(
            parent = parentContentState,
            token = tokenConverter.convert(parentContentState.feeExtraInfo.feeCryptoCurrencyStatus),
            fee = parentContentState.selectedFeeItem,
            onFeeClick = { router.push(FeeSelectorRoute.ChooseSpeed) },
        )
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun onParentStateUpdated(state: FeeSelectorUM.Content) {
        uiState.update { current ->
            current.copy(
                parent = state,
                token = tokenConverter.convert(state.feeExtraInfo.feeCryptoCurrencyStatus),
                fee = state.selectedFeeItem,
            )
        }
    }
}