package com.tangem.features.promobanners.impl.campaigns.model

import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.entity.ActivateCampaignUM
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.entity.campaignName
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class ActivateCampaignsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : Model() {

    private val params = paramsContainer.require<Params>()

    private var appCurrency: AppCurrency = AppCurrency.Default

    val uiState: StateFlow<ActivateCampaignUM>
        field = MutableStateFlow(getInitialState())

    val bridge: ChooseTokenBridge = chooseTokenBridgeFactory.create(
        modelScope = modelScope,
        settings = ChooseTokenBridge.Settings(
            title = resourceReference(R.string.common_choose_token),
            isShowMarketBlock = false,
            isShowPaymentAccount = false,
            isShowSingleCurrencyWallets = true,
        ),
    )

    init {
        getSelectedAppCurrencyUseCase.invokeOrDefault()
            .onEach { appCurrency = it }
            .launchIn(modelScope)

        bridge.onCurrencyChosen.receiveAsFlow()
            .onEach { result -> onTokenChosen(result) }
            .launchIn(modelScope)

        bridge.onClose.receiveAsFlow()
            .onEach { onChooseTokenDismiss() }
            .launchIn(modelScope)
    }

    fun onSelectTokenClick() {
        uiState.update { it.copy(isChoosingToken = true) }
    }

    fun onChooseTokenDismiss() {
        uiState.update { it.copy(isChoosingToken = false) }
    }

    fun onEnrollClick() {
        // TODO([REDACTED_TASK_KEY]): call the real campaign enrollment use case with the chosen token before proceeding.
        params.onActivated(params.campaignType)
    }

    fun onDismiss() = params.onDismiss()

    private fun onTokenChosen(result: ChooseTokenResult) {
        val tokenItem = TokenItemStateConverter(appCurrency = appCurrency).convert(result.currency)

        uiState.update { state ->
            state.copy(
                isChoosingToken = false,
                selectedToken = tokenItem,
            )
        }
    }

    private fun getInitialState(): ActivateCampaignUM {
        return ActivateCampaignUM(
            campaignName = params.campaignType.campaignName(),
            // TODO([REDACTED_TASK_KEY]): source real campaign copy.
            title = stringReference("Enroll in ${params.campaignType.campaignName()}"),
            description = stringReference(
                "Earn cashback on every swap from \$10K until the end of July.\n\n" +
                    "Rates step up with size: 0.10% from \$10K, 0.20% from \$20K, 0.50% from \$100K.\n\n",
            ),
            selectedToken = null,
            isChoosingToken = false,
        )
    }

    data class Params(
        val campaignType: CampaignType,
        val onDismiss: () -> Unit,
        val onActivated: (CampaignType) -> Unit,
    )
}