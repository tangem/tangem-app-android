package com.tangem.features.tangempay.withdraw.bank.amount.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.usecase.bankofframp.GetBankOfframpOptionsUseCase
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.common.balanceOrNull
import com.tangem.features.tangempay.common.networksOrNull
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.isZero
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankWithdrawAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val getBankOfframpOptionsUseCase: GetBankOfframpOptionsUseCase,
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
        field = MutableStateFlow(umFactory.create(draft = params.draft.value, state = state.value))

    init {
        combine(params.draft, state) { draft, amountState -> umFactory.create(draft = draft, state = amountState) }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
        loadOptions()
        observePaymentAccount()
    }

    private fun loadOptions() {
        modelScope.launch {
            getBankOfframpOptionsUseCase(params.userWalletId).fold(
                ifLeft = { error -> closeOnUnavailableOptions(reason = "failed to load offramp options: $error") },
                ifRight = { options ->
                    if (options.destinations.isEmpty()) {
                        closeOnUnavailableOptions(reason = "offramp options have no destinations")
                    } else {
                        state.update { it.copy(options = options) }
                    }
                },
            )
        }
    }

    private fun closeOnUnavailableOptions(reason: String) {
        TangemLogger.withTag(TAG).e("Bank withdraw amount step closed, $reason")
        uiMessageSender.send(
            TangemPayMessagesFactory.createErrorMessage(errorType = TangemPayDetailsErrorType.Withdraw),
        )
        router.pop()
    }

    private fun observePaymentAccount() {
        paymentAccountStatusSupplier(params.userWalletId)
            .onEach { status ->
                val networks = status.networksOrNull() ?: return@onEach
                val currencyCode = status.balanceOrNull()?.fiatBalance?.currency
                state.update { it.copy(networks = networks, currencyCode = currencyCode ?: it.currencyCode) }
            }
            .launchIn(modelScope)
    }

    private fun onValueChange(value: String) {
        if (value.isNotEmpty() && value.parseBigDecimalOrNull() == null) return
        state.update { it.copy(value = value) }
    }

    private fun onMaxClick() {
        val selection = state.value.resolve(params.draft.value) ?: return
        val max = BankWithdrawAmountValidator.maxAmount(
            limits = selection.limits,
            balance = selection.source?.fiatBalance.orZero(),
            fee = selection.destination.fee.amount,
        )
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
        val result = state.value.resolve(params.draft.value)?.toResult() ?: return
        params.onContinue(result)
        state.update { it.copy(value = null) }
    }
}

private const val TAG = "TangemPayBankWithdrawAmountModel"