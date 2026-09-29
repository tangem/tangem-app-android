package com.tangem.features.tangempay.withdraw.bank.choice.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.withdraw.bank.choice.TangemPayBankWithdrawChoiceComponent
import com.tangem.features.tangempay.withdraw.bank.choice.state.TangemPayBankWithdrawChoiceUM
import com.tangem.features.tangempay.withdraw.bank.choice.state.TangemPayBankWithdrawChoiceUMFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankWithdrawChoiceModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawChoiceComponent.Params>()

    private val umFactory = TangemPayBankWithdrawChoiceUMFactory(
        config = params.config,
        step = params.step,
        callbacks = TangemPayBankWithdrawChoiceUMFactory.Callbacks(
            onBackClick = router::pop,
            onCloseClick = params.onCloseFlow,
            onOptionClick = ::onOptionClick,
            onContinueClick = ::onContinueClick,
        ),
    )

    private val selectedId = MutableStateFlow(params.config.selectedId)

    private val isTransferTypeStep = params.step == TangemPayBankWithdrawInnerRoute.TransferType

    val uiState: StateFlow<TangemPayBankWithdrawChoiceUM>
        field = MutableStateFlow(umFactory.create(draft = params.draft.value, selectedId = selectedId.value))

    init {
        if (isTransferTypeStep) {
            analytics.send(TangemPayAnalyticsEvents.Offramp.PersonalAccountTypeScreenOpened())
        }

        combine(params.draft, selectedId) { draft, selected -> umFactory.create(draft = draft, selectedId = selected) }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    private fun onOptionClick(id: String) {
        selectedId.value = id
    }

    private fun onContinueClick() {
        // The button is already disabled without a selection; the guard also covers a click racing the state update.
        val selected = selectedId.value ?: return

        if (isTransferTypeStep) {
            analytics.send(
                TangemPayAnalyticsEvents.Offramp.PersonalAccountTypeContinueClicked(
                    isThirdParty = BankWithdrawChoices.isThirdParty(selectedId = selected),
                ),
            )
        }

        params.onContinue(selected)
    }
}