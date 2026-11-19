package com.tangem.features.tangempay.withdraw.bank.preview.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.wallet.isColdWallet
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.AmountErrorReason
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAnalytics
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.preview.TangemPayBankWithdrawPreviewComponent
import com.tangem.features.tangempay.withdraw.bank.preview.completion.BankWithdrawCompletionFactory
import com.tangem.features.tangempay.withdraw.bank.preview.completion.CompletionState
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUM
import com.tangem.features.tangempay.withdraw.bank.preview.state.TangemPayBankWithdrawPreviewUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.time.CurrentTimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayBankWithdrawPreviewModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    completionFactory: BankWithdrawCompletionFactory,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    withdrawAnalyticsFactory: BankWithdrawAnalytics.Factory,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val currentTimeProvider: CurrentTimeProvider,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawPreviewComponent.Params>()

    private val withdrawAnalytics = withdrawAnalyticsFactory.create(sourceAccount = params.sourceAccount)

    private val completion = completionFactory.create(
        sourceAccount = params.sourceAccount,
        scope = modelScope,
        callbacks = BankWithdrawCompletionFactory.Callbacks(
            onOrderCreated = params.onOrderCreated,
            onDestinationChanged = params.onDestinationChanged,
            onNetworkFeeChanged = params.onNetworkFeeChanged,
            onQuoteExpired = ::reportQuoteExpired,
            newRecipientListener = params.newRecipientListener,
        ),
    )

    private val umFactory = TangemPayBankWithdrawPreviewUMFactory(
        sourceAccount = params.sourceAccount,
        callbacks = TangemPayBankWithdrawPreviewUMFactory.Callbacks(
            onBackClick = ::onBackClick,
            onCloseClick = ::onCloseClick,
            onSendClick = ::onSendClick,
            onRetryClick = ::onRetryClick,
            onEditClick = ::onEditClick,
            onRailClick = ::onRailClick,
        ),
    )

    private val state = MutableStateFlow(PreviewState(isColdWallet = resolveIsColdWallet(), isBalanceHidden = true))

    /** Shared by prepare and send, so a quote request in progress never runs beside another for a newer draft. */
    private val completionJobHolder = JobHolder()

    private var isQuoteFailure = false

    private var isScreenVisible = false

    private var expiredQuoteId: String? = null

    private val isSending: Boolean get() = state.value.isSending

    val uiState: StateFlow<TangemPayBankWithdrawPreviewUM>
        field = MutableStateFlow(umFactory.create(draft = params.draft.value, state = state.value))

    init {
        combine(params.draft, state) { draft, previewState -> umFactory.create(draft = draft, state = previewState) }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
        getBalanceHidingSettingsUseCase.isBalanceHidden()
            .onEach { isBalanceHidden -> state.update { it.copy(isBalanceHidden = isBalanceHidden) } }
            .launchIn(modelScope)
        lockedQuotes()
            .onEach { quote -> state.update { it.copy(quote = quote) } }
            .launchIn(modelScope)
        completion.quote
            .filterNotNull()
            .onEach(params.onQuoteShown)
            .launchIn(modelScope)
        requoteWhenLockRunsOut()
        withdrawAnalytics.onConfirmationScreenOpened(params.draft.value)
        params.draft
            .map(BankWithdrawDraft::toOrderRequest)
            .distinctUntilChanged()
            .onEach { prepare() }
            .launchIn(modelScope)
    }

    /** The wallet cannot change while the wizard is open, and a lookup failure keeps the historic card icon. */
    private fun resolveIsColdWallet(): Boolean = getUserWalletUseCase(params.userWalletId).fold(
        ifLeft = { error ->
            TangemLogger.withTag(TAG).e("Failed to resolve the wallet of a bank withdraw: $error")
            true
        },
        ifRight = { wallet -> wallet.isColdWallet },
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun lockedQuotes(): Flow<LockedQuote?> = completion.quote.flatMapLatest { quote ->
        when {
            quote == null -> flowOf(null)
            !quote.isLockTrusted(currentTimeProvider.nowMillis()) -> flowOf(LockedQuote(quote, secondsLeft = null))
            else -> secondsLeftUntil(quote.sendDeadline).map { secondsLeft -> LockedQuote(quote, secondsLeft) }
        }
    }

    private fun secondsLeftUntil(deadline: DateTime): Flow<Long> = flow {
        var millisLeft = deadline.millis - currentTimeProvider.nowMillis()
        while (millisLeft > 0) {
            val secondsLeft = (millisLeft + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
            emit(secondsLeft)
            delay(timeMillis = millisLeft - (secondsLeft - 1) * MILLIS_PER_SECOND)
            millisLeft = deadline.millis - currentTimeProvider.nowMillis()
        }
        emit(0L)
    }

    /**
     * Terms that ran out are replaced as soon as the screen is idle, so Pay never sends on them: on the spot, or once a
     * send in flight ends without sending. A failure on screen keeps them, and Retry replaces them instead.
     */
    private fun requoteWhenLockRunsOut() {
        state
            .map { it.completion == CompletionState.Idle && it.quote?.secondsLeft == 0L }
            .distinctUntilChanged()
            .filter { hasRunOut -> hasRunOut }
            .onEach {
                reportQuoteExpired()
                prepare()
            }
            .launchIn(modelScope)
    }

    private fun prepare() {
        if (isSending) return
        val draft = params.draft.value
        modelScope.launch {
            completion.prepare(draft) { completionState ->
                isQuoteFailure = completionState is CompletionState.Failed
                if (isQuoteFailure) reportQuoteError(AmountErrorReason.QUOTE_UNAVAILABLE)
                state.update { it.copy(completion = completionState) }
            }
        }.saveIn(completionJobHolder)
    }

    fun onScreenVisibilityChanged(isVisible: Boolean) {
        isScreenVisible = isVisible
    }

    private fun reportQuoteExpired() {
        if (!isScreenVisible) return
        val quoteId = state.value.quote?.quote?.quoteId
        if (quoteId != null && quoteId == expiredQuoteId) return
        expiredQuoteId = quoteId
        reportQuoteError(AmountErrorReason.QUOTE_EXPIRED)
    }

    private fun reportQuoteError(reason: AmountErrorReason) {
        val rail = params.draft.value.destination?.rail ?: return
        withdrawAnalytics.onAmountErrorShown(rail = rail, reason = reason)
    }

    private fun onSendClick() {
        if (isSending) return
        withdrawAnalytics.onSendClicked(params.draft.value)
        complete()
    }

    /** Retry after terms that never arrived only asks for them again: nothing was sent, so no send is reported. */
    private fun onRetryClick() {
        if (isSending) return
        if (isQuoteFailure) prepare() else complete()
    }

    private fun complete() {
        isQuoteFailure = false
        val draft = params.draft.value
        modelScope.launch {
            completion.complete(draft = draft) { completionState -> onCompletionState(completionState, draft) }
        }.saveIn(completionJobHolder)
    }

    private fun onCompletionState(completionState: CompletionState, draft: BankWithdrawDraft) {
        state.update { it.copy(completion = completionState) }
        when (completionState) {
            CompletionState.Completed -> {
                withdrawAnalytics.onSendAttempted(draft)
                params.onCompleted()
            }
            CompletionState.WithdrawalInProgress -> uiMessageSender.send(
                TangemPayMessagesFactory.createErrorMessage(errorType = TangemPayDetailsErrorType.WithdrawInProgress),
            )
            is CompletionState.Failed -> onCompletionFailed(failed = completionState, draft = draft)
            CompletionState.Idle,
            CompletionState.Sending,
            -> Unit
        }
    }

    private fun onCompletionFailed(failed: CompletionState.Failed, draft: BankWithdrawDraft) {
        if (failed.isQuoteFailure) {
            reportQuoteError(AmountErrorReason.QUOTE_UNAVAILABLE)
        } else {
            withdrawAnalytics.onSendAttempted(draft)
            withdrawAnalytics.onSendFailed(errorCode = failed.errorCode, rail = draft.destination?.rail)
        }
        if (failed.failure == PreviewFailure.TransferOutcomeUnknown) params.onTransferOutcomeUnknown()
    }

    private fun onBackClick() {
        if (isSending) return
        router.pop()
    }

    private fun onCloseClick() {
        if (isSending) return
        params.onCloseFlow()
    }

    private fun onEditClick(target: BankWithdrawEditTarget) {
        if (isSending) return
        editEvent(target)?.let(withdrawAnalytics::sendPaymentAccountOnly)
        router.push(TangemPayBankWithdrawInnerRoute.Edit(target))
    }

    /** The rail is picked on the amount step, so the row reopens it. */
    private fun onRailClick() {
        if (isSending) return
        withdrawAnalytics.sendPaymentAccountOnly(TangemPayAnalyticsEvents.Offramp.RailSelectorClicked())
        onEditClick(BankWithdrawEditTarget.Amount)
    }

    /** The amount step has no event of its own: the rail row reports its own tap, and the amount card none. */
    private fun editEvent(target: BankWithdrawEditTarget): TangemPayAnalyticsEvents? = when (target) {
        BankWithdrawEditTarget.RecipientDetails ->
            TangemPayAnalyticsEvents.Offramp.RecipientDetailsOnConfirmationClicked()
        BankWithdrawEditTarget.BankDetails ->
            TangemPayAnalyticsEvents.Offramp.RecipientBankDetailsOnConfirmationClicked()
        BankWithdrawEditTarget.Amount -> null
    }
}

private const val TAG = "TangemPayBankWithdrawPreviewModel"
private const val MILLIS_PER_SECOND = 1_000L