package com.tangem.features.tangempay.withdraw.bank.preview.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.wallet.isColdWallet
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawAnalytics
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    analytics: AnalyticsEventHandler,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawPreviewComponent.Params>()

    private val withdrawAnalytics = BankWithdrawAnalytics(sourceAccount = params.sourceAccount, analytics = analytics)

    private val completion = completionFactory.create(
        sourceAccount = params.sourceAccount,
        scope = modelScope,
        onOrderCreated = params.onOrderCreated,
        onDestinationChanged = params.onDestinationChanged,
        onNetworkFeeChanged = params.onNetworkFeeChanged,
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

    private val sendJobHolder = JobHolder()

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
        withdrawAnalytics.onConfirmationScreenOpened()
    }

    /** The wallet cannot change while the wizard is open, and a lookup failure keeps the historic card icon. */
    private fun resolveIsColdWallet(): Boolean = getUserWalletUseCase(params.userWalletId).fold(
        ifLeft = { error ->
            TangemLogger.withTag(TAG).e("Failed to resolve the wallet of a bank withdraw: $error")
            true
        },
        ifRight = { wallet -> wallet.isColdWallet },
    )

    private fun onSendClick() {
        if (isSending) return
        withdrawAnalytics.onSendClicked(params.draft.value)
        complete()
    }

    private fun onRetryClick() {
        if (isSending) return
        complete()
    }

    private fun complete() {
        val draft = params.draft.value
        modelScope.launch {
            completion.complete(draft = draft, onState = ::onCompletionState)
        }.saveIn(sendJobHolder)
    }

    private fun onCompletionState(completionState: CompletionState) {
        state.update { it.copy(completion = completionState) }
        when (completionState) {
            CompletionState.Completed -> params.onCompleted()
            CompletionState.WithdrawalInProgress -> uiMessageSender.send(
                TangemPayMessagesFactory.createErrorMessage(errorType = TangemPayDetailsErrorType.WithdrawInProgress),
            )
            is CompletionState.Failed -> onCompletionFailed(completionState)
            CompletionState.Idle,
            CompletionState.Sending,
            -> Unit
        }
    }

    private fun onCompletionFailed(failed: CompletionState.Failed) {
        withdrawAnalytics.onSendFailed(errorCode = failed.errorCode)
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