package com.tangem.features.tangempay.withdraw.bank.preview.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.wallet.isColdWallet
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpSignResult
import com.tangem.domain.pay.usecase.bankofframp.AwaitBankOfframpSignatureUseCase
import com.tangem.domain.pay.usecase.bankofframp.CancelBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.CreateBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.SignBankOfframpOrderUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.tangempay.account.TangemPayAccountDetailsInnerRoute
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.bank.preview.TangemPayBankWithdrawPreviewComponent
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
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayBankWithdrawPreviewModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val createBankOfframpOrderUseCase: CreateBankOfframpOrderUseCase,
    private val awaitBankOfframpSignatureUseCase: AwaitBankOfframpSignatureUseCase,
    private val signBankOfframpOrderUseCase: SignBankOfframpOrderUseCase,
    private val cancelBankOfframpOrderUseCase: CancelBankOfframpOrderUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankWithdrawPreviewComponent.Params>()

    private val umFactory = TangemPayBankWithdrawPreviewUMFactory(
        callbacks = TangemPayBankWithdrawPreviewUMFactory.Callbacks(
            onBackClick = ::onBackClick,
            onCloseClick = ::onCloseClick,
            onSendClick = ::onSendClick,
            onRetryClick = ::onRetryClick,
            onEditClick = ::onEditClick,
            onRailClick = ::onRailClick,
        ),
    )

    private val state = MutableStateFlow(PreviewState(isColdWallet = resolveIsColdWallet()))

    private val sendJobHolder = JobHolder()

    /**
     * Generated on the first Send and shared by every call of that attempt — creating the order and signing it — so
     * neither a retry nor a resubmitted signature can produce a second withdrawal. Cleared once the attempt can no
     * longer be resumed with it: the transfer is signed, the order was abandoned, or the backend refused the order
     * the key belongs to. The next Send then starts a new attempt with a new key.
     */
    private var idempotencyKey: String? = null

    private val isSending: Boolean get() = state.value.isSending

    val uiState: StateFlow<TangemPayBankWithdrawPreviewUM>
        field = MutableStateFlow(umFactory.create(draft = params.draft.value, state = state.value))

    init {
        combine(params.draft, state) { draft, previewState -> umFactory.create(draft = draft, state = previewState) }
            .onEach { uiState.value = it }
            .launchIn(modelScope)
        analytics.send(TangemPayAnalyticsEvents.Offramp.ConfirmationScreenOpened())
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
        analytics.send(TangemPayAnalyticsEvents.Offramp.SendOnConfirmationClicked())
        send()
    }

    private fun send() {
        if (isSending) return
        val request = params.draft.value.toOrderRequest() ?: return
        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
        state.update { it.copy(send = SendState.CreatingOrder, failure = null, resumableOrderId = null) }
        modelScope.launch {
            createBankOfframpOrderUseCase(userWalletId = params.userWalletId, request = request, idempotencyKey = key)
                .fold(ifLeft = ::onSendFailed, ifRight = { result -> onOrderCreated(result) })
        }.saveIn(sendJobHolder)
    }

    private suspend fun onOrderCreated(result: BankOfframpOrderResult) {
        params.onOrderCreated(result)
        awaitSignature(orderId = result.orderId)
    }

    /** The backend prepares the transfer before it asks for a signature, so the order is polled until it does. */
    private suspend fun awaitSignature(orderId: String) {
        state.update { it.copy(send = SendState.AwaitingSignature(orderId), failure = null, resumableOrderId = null) }
        awaitBankOfframpSignatureUseCase(userWalletId = params.userWalletId, orderId = orderId).fold(
            ifLeft = { error -> onOrderFailed(error = error, orderId = orderId) },
            ifRight = { payloadHash -> sign(orderId = orderId, payloadHash = payloadHash) },
        )
    }

    private suspend fun sign(orderId: String, payloadHash: String) {
        state.update { it.copy(send = SendState.Signing(orderId)) }
        signBankOfframpOrderUseCase(
            userWalletId = params.userWalletId,
            orderId = orderId,
            payloadHash = payloadHash,
            idempotencyKey = requireNotNull(idempotencyKey) { "Signing an order created without an idempotency key" },
        ).fold(
            ifLeft = { error -> onOrderFailed(error = error, orderId = orderId) },
            ifRight = { result ->
                when (result) {
                    BankOfframpSignResult.Signed -> onOrderSigned()
                    BankOfframpSignResult.Cancelled -> onSignatureCancelled(orderId = orderId)
                }
            },
        )
    }

    private fun onOrderSigned() {
        idempotencyKey = null
        state.update { it.copy(send = SendState.Idle) }
        params.onOrderSigned()
    }

    /** Aborting the card prompt abandons the transfer, so the order is cancelled and the summary becomes editable. */
    private suspend fun onSignatureCancelled(orderId: String) {
        idempotencyKey = null
        cancelOrder(orderId = orderId)
        state.update { it.copy(send = SendState.Idle, failure = null, resumableOrderId = null) }
    }

    private suspend fun cancelOrder(orderId: String) {
        cancelBankOfframpOrderUseCase(userWalletId = params.userWalletId, orderId = orderId).onLeft { error ->
            TangemLogger.withTag(TAG).e("Failed to cancel bank withdraw order $orderId: $error")
        }
    }

    private fun onSendFailed(error: BankOfframpError) {
        TangemLogger.withTag(TAG).e("Bank withdraw order creation failed: $error")
        if (error == BankOfframpError.WithdrawalInProgress) {
            idempotencyKey = null
            state.update { it.copy(send = SendState.Idle) }
            uiMessageSender.send(
                TangemPayMessagesFactory.createErrorMessage(errorType = TangemPayDetailsErrorType.WithdrawInProgress),
            )
            return
        }
        fail(failure = error.toFailure(), resumableOrderId = null)
    }

    /**
     * The order already exists here: only a timeout leaves it alive, so only then may Retry resume it. Anything
     * else means the backend gave up on this order and a retry has to create a new one, so this one is cancelled —
     * outside the send job, which a Retry would cancel with it — before it can settle behind the user's back.
     */
    private fun onOrderFailed(error: BankOfframpError, orderId: String) {
        TangemLogger.withTag(TAG).e("Bank withdraw order $orderId failed: $error")
        val isResumable = error == BankOfframpError.PollingTimeout
        if (!isResumable) {
            idempotencyKey = null
            modelScope.launch { cancelOrder(orderId = orderId) }
        }
        fail(failure = error.toFailure(), resumableOrderId = orderId.takeIf { isResumable })
    }

    private fun fail(failure: PreviewFailure, resumableOrderId: String?) {
        state.update { it.copy(send = SendState.Idle, failure = failure, resumableOrderId = resumableOrderId) }
        analytics.send(TangemPayAnalyticsEvents.Offramp.SendErrorScreenOpened())
    }

    private fun onRetryClick() {
        if (isSending) return
        val orderId = state.value.resumableOrderId ?: return send()
        modelScope.launch { awaitSignature(orderId = orderId) }.saveIn(sendJobHolder)
    }

    private fun onBackClick() {
        if (isSending) return
        router.pop()
    }

    private fun onCloseClick() {
        if (isSending) return
        router.popTo(TangemPayAccountDetailsInnerRoute.AccountDetails)
    }

    private fun onEditClick(target: BankWithdrawEditTarget) {
        if (isSending) return
        editEvent(target)?.let(analytics::send)
        router.push(TangemPayBankWithdrawInnerRoute.Edit(target))
    }

    /** The rail is picked on the amount step, so the row reopens it. */
    private fun onRailClick() {
        if (isSending) return
        analytics.send(TangemPayAnalyticsEvents.Offramp.RailSelectorClicked())
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

private fun BankOfframpError.toFailure(): PreviewFailure = when (this) {
    BankOfframpError.RecipientRegionUnsupported -> PreviewFailure.RecipientRegion
    BankOfframpError.SenderRegionUnsupported -> PreviewFailure.SenderRegion
    BankOfframpError.BusinessUnsupported -> PreviewFailure.Business
    BankOfframpError.UnsupportedConfiguration -> PreviewFailure.UnsupportedConfiguration
    BankOfframpError.Validation,
    BankOfframpError.PollingTimeout,
    BankOfframpError.WithdrawalInProgress,
    is BankOfframpError.Unknown,
    -> PreviewFailure.General
}

private const val TAG = "TangemPayBankWithdrawPreviewModel"