package com.tangem.features.tangempay.withdraw.bank.amount.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUMFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.isZero
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.math.RoundingMode
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankWithdrawAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawAmountComponent.Params>()

    private val umFactory = TangemPayBankWithdrawAmountUMFactory(
        step = params.step,
        callbacks = TangemPayBankWithdrawAmountUMFactory.Callbacks(
            onBackClick = router::pop,
            onValueChange = ::onValueChange,
            onMaxClick = ::onMaxClick,
            onSourceClick = { openSheet(AmountSheet.TOKENS) },
            onSourceSelected = ::onSourceSelected,
            onRouteClick = { openSheet(AmountSheet.ROUTES) },
            onRouteSelected = ::onRouteSelected,
            onSheetDismiss = ::closeSheet,
            onContinueClick = ::onContinueClick,
        ),
    )

    private val state = MutableStateFlow(AmountState())

    val uiState: StateFlow<TangemPayBankWithdrawAmountUM>
        field = MutableStateFlow(createUM(draft = params.draft.value, options = params.options.value, state.value))

    init {
        combine(params.draft, params.options, state) { draft, options, amountState ->
            createUM(draft = draft, options = options, state = amountState)
        }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    private fun createUM(
        draft: BankWithdrawDraft,
        options: BankWithdrawOptions,
        state: AmountState,
    ): TangemPayBankWithdrawAmountUM = umFactory.create(
        draft = draft,
        selection = state.resolve(draft = draft, options = options),
        sheet = state.sheet,
    )

    private fun onValueChange(value: String) {
        if (value.isNotEmpty() && value.parseBigDecimalOrNull() == null) return
        state.update { it.copy(value = value) }
    }

    private fun onMaxClick() {
        val selection = currentSelection()
        val max = selection.maxAmount
        val value = if (max.isZero()) {
            ""
        } else {
            max.parseBigDecimal(
                decimals = selection.currency.defaultFractionDigits,
                roundingMode = RoundingMode.DOWN,
            )
        }
        state.update { it.copy(value = value) }
    }

    private fun onSourceSelected(sourceId: String) {
        state.update { it.copy(selectedSourceId = sourceId, sheet = null) }
    }

    private fun onRouteSelected(rail: BankRail) {
        state.update { it.copy(selectedRail = rail, sheet = null) }
    }

    private fun openSheet(sheet: AmountSheet) {
        state.update { it.copy(sheet = sheet) }
    }

    private fun closeSheet() {
        state.update { it.copy(sheet = null) }
    }

    private fun onContinueClick() {
        val result = currentSelection().toResult() ?: return
        params.onContinue(result)
        state.update { it.copy(value = null) }
    }

    private fun currentSelection(): AmountSelection =
        state.value.resolve(draft = params.draft.value, options = params.options.value)
}