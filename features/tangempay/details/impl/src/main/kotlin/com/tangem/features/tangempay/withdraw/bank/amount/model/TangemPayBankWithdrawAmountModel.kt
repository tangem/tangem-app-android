package com.tangem.features.tangempay.withdraw.bank.amount.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.AmountErrorReason
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.amount.TangemPayBankWithdrawAmountComponent
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUM
import com.tangem.features.tangempay.withdraw.bank.amount.state.TangemPayBankWithdrawAmountUMFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAnalytics
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isZero
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayBankWithdrawAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    withdrawAnalyticsFactory: BankWithdrawAnalytics.Factory,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val networkFeeLoader: BankWithdrawNetworkFeeLoader,
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawAmountComponent.Params>()

    private val isEditing = params.step is TangemPayBankWithdrawInnerRoute.Edit

    private val withdrawAnalytics = withdrawAnalyticsFactory.create(sourceAccount = params.sourceAccount)

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
            onRetryClick = ::loadNetworkFee,
        ),
        isMxnEnabled = moneyAccountFeatureToggles.isMxnEnabled,
    )

    private val state = MutableStateFlow(AmountState())

    private val networkFeeJobHolder = JobHolder()

    private var previousValidation: BankWithdrawAmountValidator.Result? = null

    private val shownAmountError = MutableStateFlow<Pair<BankRail, AmountErrorReason>?>(null)

    private val isScreenVisible = MutableStateFlow(false)

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
        reportScreenOpened()
        reportAmountErrors()
        loadNetworkFee()
        combine(
            flow = params.draft,
            flow2 = params.options,
            flow3 = state,
            flow4 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        ) { draft, options, amountState, isBalanceHidden ->
            val selection = amountState.resolve(draft = draft, options = options, isEditing = isEditing)
            trackInsufficientFunds(selection.validation)
            umFactory.create(
                draft = draft,
                selection = selection,
                sheet = amountState.sheet,
                networkFee = amountState.networkFee,
                isBalanceHidden = isBalanceHidden,
            ).also { um ->
                shownAmountError.value = selection.amountError().takeIf { um is TangemPayBankWithdrawAmountUM.Content }
            }
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

    fun onScreenVisibilityChanged(isVisible: Boolean) {
        isScreenVisible.value = isVisible
    }

    private fun reportScreenOpened() {
        modelScope.launch {
            val isSavedEntry = params.draft.value.entry is BankWithdrawEntry.Saved
            if (moneyAccountFeatureToggles.isMxnEnabled && isSavedEntry) params.isSavedAccountsLoaded.first { it }
            val isRailKnown = !isSavedEntry || params.draft.value.recipient != null
            withdrawAnalytics.onAmountScreenOpened(rail = currentSelection().destination.rail.takeIf { isRailKnown })
        }
    }

    @OptIn(FlowPreview::class)
    private fun reportAmountErrors() {
        combine(shownAmountError, isScreenVisible) { error, isVisible -> error.takeIf { isVisible } }
            .debounce(AMOUNT_ERROR_SHOWN_DELAY_MILLIS)
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { (rail, reason) -> withdrawAnalytics.onAmountErrorShown(rail = rail, reason = reason) }
            .launchIn(modelScope)
    }

    private fun AmountSelection.amountError(): Pair<BankRail, AmountErrorReason>? {
        val reason = when (validation) {
            BankWithdrawAmountValidator.Result.NotEnoughFunds -> AmountErrorReason.ABOVE_BALANCE
            BankWithdrawAmountValidator.Result.OutOfRange -> {
                val typed = amount
                AmountErrorReason.BELOW_MINIMUM.takeIf { typed != null && typed < minAmount }
            }
            BankWithdrawAmountValidator.Result.Empty,
            BankWithdrawAmountValidator.Result.Valid,
            -> null
        }
        return reason?.let { destination.rail to it }
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
        state.value.resolve(draft = params.draft.value, options = params.options.value, isEditing = isEditing)
}

private const val TAG = "TangemPayBankWithdrawAmountModel"
private const val AMOUNT_ERROR_SHOWN_DELAY_MILLIS = 1_000L