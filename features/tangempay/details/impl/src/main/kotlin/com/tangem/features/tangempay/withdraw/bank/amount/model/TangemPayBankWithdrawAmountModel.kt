package com.tangem.features.tangempay.withdraw.bank.amount.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUMFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAnalytics
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankWithdrawAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    analytics: AnalyticsEventHandler,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val networkFeeLoader: BankWithdrawNetworkFeeLoader,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawAmountComponent.Params>()

    private val withdrawAnalytics = BankWithdrawAnalytics(sourceAccount = params.sourceAccount, analytics = analytics)

    /** An amount restored from the draft was entered on an earlier visit, so it must not be reported again. */
    private var isAmountEntered = params.draft.value.amount != null

    private val umFactory = TangemPayBankWithdrawAmountUMFactory(
        step = params.step,
        sourceAccount = params.sourceAccount,
        callbacks = TangemPayBankWithdrawAmountUMFactory.Callbacks(
            onBackClick = router::pop,
            onCloseClick = params.onCloseFlow,
            onValueChange = ::onValueChange,
            onMaxClick = ::onMaxClick,
            onSourceClick = ::onSourceClick,
            onSourceSelected = ::onSourceSelected,
            onRouteClick = ::onRouteClick,
            onRouteSelected = ::onRouteSelected,
            onSheetDismiss = ::closeSheet,
            onContinueClick = ::onContinueClick,
            onRetryFeeClick = ::loadNetworkFee,
        ),
    )

    private val state = MutableStateFlow(AmountState())

    private val networkFeeJobHolder = JobHolder()

    private var previousValidation: BankWithdrawAmountValidator.Result? = null

    val uiState: StateFlow<TangemPayBankWithdrawAmountUM>
        field = MutableStateFlow(
            umFactory.create(
                draft = params.draft.value,
                selection = currentSelection(),
                sheet = state.value.sheet,
                networkFee = state.value.networkFee,
                isBalanceHidden = true,
            ),
        )

    init {
        withdrawAnalytics.onAmountScreenOpened()
        loadNetworkFee()
        combine(
            flow = params.draft,
            flow2 = params.options,
            flow3 = state,
            flow4 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        ) { draft, options, amountState, isBalanceHidden ->
            val selection = amountState.resolve(draft = draft, options = options)
            trackInsufficientFunds(selection.validation)
            umFactory.create(
                draft = draft,
                selection = selection,
                sheet = amountState.sheet,
                networkFee = amountState.networkFee,
                isBalanceHidden = isBalanceHidden,
            )
        }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    private fun loadNetworkFee() {
        state.update { it.copy(networkFee = NetworkFeeState.Loading) }
        modelScope.launch {
            val networkFee = networkFeeLoader.load(
                account = params.sourceAccount,
                token = currentSelection().source.token,
            ).fold(
                ifLeft = { error ->
                    if (error == GetFeeError.GaslessError.NotEnoughFunds) {
                        TangemLogger.withTag(TAG).i("The balance does not cover the network fee of a bank withdraw")
                        NetworkFeeState.NotEnoughFunds
                    } else {
                        TangemLogger.withTag(TAG).e("Failed to load the network fee of a bank withdraw: $error")
                        NetworkFeeState.Unreachable
                    }
                },
                ifRight = NetworkFeeState::Loaded,
            )
            state.update { it.copy(networkFee = networkFee) }
        }.saveIn(networkFeeJobHolder)
    }

    /** Reported on every entry into the shortage, so re-entering it after a valid amount reports it again. */
    private fun trackInsufficientFunds(validation: BankWithdrawAmountValidator.Result) {
        val wasNotEnoughFunds = previousValidation == BankWithdrawAmountValidator.Result.NotEnoughFunds
        previousValidation = validation
        if (!wasNotEnoughFunds && validation == BankWithdrawAmountValidator.Result.NotEnoughFunds) {
            withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.InsufficientFundsStateShowed())
        }
    }

    private fun onValueChange(value: String) {
        val amount = value.parseBigDecimalOrNull()
        if (value.isNotEmpty() && amount == null) return
        trackAmountEntered(amount)
        state.update { it.copy(value = value) }
    }

    private fun onMaxClick() {
        val selection = currentSelection()
        val max = selection.maxInputAmount
        val value = if (max.isZero()) {
            ""
        } else {
            max.parseBigDecimal(
                decimals = selection.currency.defaultFractionDigits,
                roundingMode = RoundingMode.DOWN,
            )
        }
        trackAmountEntered(max)
        state.update { it.copy(value = value) }
    }

    private fun trackAmountEntered(amount: BigDecimal?) {
        if (isAmountEntered || amount == null || !amount.isPositive()) return
        isAmountEntered = true
        withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.AmountEntered())
    }

    private fun onSourceClick() {
        withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.CoinSelectorClicked())
        withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.CoinSelectionSheetOpened())
        openSheet(AmountSheet.TOKENS)
    }

    private fun onSourceSelected(sourceId: String) {
        if (sourceId != currentSelection().source.id) {
            withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.CoinInSelectionSheetClicked())
        }
        state.update { it.copy(selectedSourceId = sourceId, sheet = null) }
    }

    private fun onRouteClick() {
        withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.RailSelectorClicked())
        withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.RailSelectionSheetOpened())
        openSheet(AmountSheet.ROUTES)
    }

    private fun onRouteSelected(rail: BankRail) {
        if (rail != currentSelection().destination.rail) {
            withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.RailInSelectionSheetClicked())
        }
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
        withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.AmountContinueClicked())
        params.onContinue(result)
        state.update { it.copy(value = null) }
    }

    private fun currentSelection(): AmountSelection =
        state.value.resolve(draft = params.draft.value, options = params.options.value)
}

private const val TAG = "TangemPayBankWithdrawAmountModel"