package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpSignResult
import com.tangem.domain.pay.usecase.bankofframp.AwaitBankOfframpSignatureUseCase
import com.tangem.domain.pay.usecase.bankofframp.CancelBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.CreateBankOfframpOrderUseCase
import com.tangem.domain.pay.usecase.bankofframp.SignBankOfframpOrderUseCase
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.preview.model.toFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.toOrderRequest
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

@Suppress("LongParameterList")
internal class OrderBankWithdrawCompletion @AssistedInject constructor(
    @Assisted private val userWalletId: UserWalletId,
    @Assisted private val scope: CoroutineScope,
    @Assisted private val onOrderCreated: (BankOfframpOrderResult) -> Unit,
    private val createBankOfframpOrderUseCase: CreateBankOfframpOrderUseCase,
    private val awaitBankOfframpSignatureUseCase: AwaitBankOfframpSignatureUseCase,
    private val signBankOfframpOrderUseCase: SignBankOfframpOrderUseCase,
    private val cancelBankOfframpOrderUseCase: CancelBankOfframpOrderUseCase,
) : BankWithdrawCompletion {

    /**
     * Generated on the first Send and shared by every call of that attempt — creating the order and signing it — so
     * neither a retry nor a resubmitted signature can produce a second withdrawal. Cleared once the attempt can no
     * longer be resumed with it: the transfer is signed, the order was abandoned, or the backend refused the order
     * the key belongs to. The next Send then starts a new attempt with a new key.
     */
    private var idempotencyKey: String? = null

    /** Set when a failure left a live order behind, so Retry resumes that order instead of creating a second one. */
    private var resumableOrderId: String? = null

    override suspend fun complete(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {
        val request = draft.toOrderRequest() ?: return
        onState(CompletionState.Sending)
        val orderId = resumableOrderId
        if (orderId == null) {
            createOrder(request = request, onState = onState)
        } else {
            awaitSignature(orderId = orderId, request = request, onState = onState)
        }
    }

    private suspend fun createOrder(request: BankOfframpOrderRequest, onState: (CompletionState) -> Unit) {
        val key = idempotencyKey ?: UUID.randomUUID().toString().also { idempotencyKey = it }
        createBankOfframpOrderUseCase(userWalletId = userWalletId, request = request, idempotencyKey = key).fold(
            ifLeft = { error -> onOrderCreationFailed(error = error, onState = onState) },
            ifRight = { result ->
                onOrderCreated(result)
                awaitSignature(orderId = result.orderId, request = request, onState = onState)
            },
        )
    }

    /** The backend prepares the transfer before it asks for a signature, so the order is polled until it does. */
    private suspend fun awaitSignature(
        orderId: String,
        request: BankOfframpOrderRequest,
        onState: (CompletionState) -> Unit,
    ) {
        resumableOrderId = null
        awaitBankOfframpSignatureUseCase(userWalletId = userWalletId, orderId = orderId, request = request).fold(
            ifLeft = { error -> onOrderFailed(error = error, orderId = orderId, onState = onState) },
            ifRight = { payloadHash -> sign(orderId = orderId, payloadHash = payloadHash, onState = onState) },
        )
    }

    private suspend fun sign(orderId: String, payloadHash: String, onState: (CompletionState) -> Unit) {
        signBankOfframpOrderUseCase(
            userWalletId = userWalletId,
            orderId = orderId,
            payloadHash = payloadHash,
            idempotencyKey = requireNotNull(idempotencyKey) { "Signing an order created without an idempotency key" },
        ).fold(
            ifLeft = { error -> onOrderFailed(error = error, orderId = orderId, onState = onState) },
            ifRight = { result ->
                when (result) {
                    BankOfframpSignResult.Signed -> onOrderSigned(onState)
                    BankOfframpSignResult.Cancelled -> onSignatureCancelled(orderId = orderId, onState = onState)
                }
            },
        )
    }

    private fun onOrderSigned(onState: (CompletionState) -> Unit) {
        idempotencyKey = null
        onState(CompletionState.Completed)
    }

    /** Aborting the card prompt abandons the transfer, so the order is cancelled and the summary becomes editable. */
    private suspend fun onSignatureCancelled(orderId: String, onState: (CompletionState) -> Unit) {
        idempotencyKey = null
        cancelOrder(orderId = orderId)
        onState(CompletionState.Idle)
    }

    private suspend fun cancelOrder(orderId: String) {
        cancelBankOfframpOrderUseCase(userWalletId = userWalletId, orderId = orderId).onLeft { error ->
            TangemLogger.withTag(TAG).e("Failed to cancel bank withdraw order $orderId: $error")
        }
    }

    private fun onOrderCreationFailed(error: BankOfframpError, onState: (CompletionState) -> Unit) {
        TangemLogger.withTag(TAG).e("Bank withdraw order creation failed: $error")
        if (error == BankOfframpError.WithdrawalInProgress) {
            idempotencyKey = null
            onState(CompletionState.WithdrawalInProgress)
            return
        }
        val isOutcomeUnknown = error is BankOfframpError.Unknown && error.isOutcomeUnknown
        if (!isOutcomeUnknown) idempotencyKey = null
        fail(error = error, resumableOrderId = null, onState = onState)
    }

    /**
     * The order already exists here: only a timeout leaves it alive, so only then may Retry resume it. Anything
     * else means the backend gave up on this order and a retry has to create a new one, so this one is cancelled —
     * outside the send job, which a Retry would cancel with it — before it can settle behind the user's back.
     */
    private fun onOrderFailed(error: BankOfframpError, orderId: String, onState: (CompletionState) -> Unit) {
        TangemLogger.withTag(TAG).e("Bank withdraw order $orderId failed: $error")
        val isResumable = error == BankOfframpError.PollingTimeout
        if (!isResumable) {
            idempotencyKey = null
            scope.launch { cancelOrder(orderId = orderId) }
        }
        fail(error = error, resumableOrderId = orderId.takeIf { isResumable }, onState = onState)
    }

    private fun fail(error: BankOfframpError, resumableOrderId: String?, onState: (CompletionState) -> Unit) {
        this.resumableOrderId = resumableOrderId
        onState(CompletionState.Failed(error.toFailure()))
    }

    @AssistedFactory
    interface Factory {
        fun create(
            userWalletId: UserWalletId,
            scope: CoroutineScope,
            onOrderCreated: (BankOfframpOrderResult) -> Unit,
        ): OrderBankWithdrawCompletion
    }
}

private const val TAG = "OrderBankWithdrawCompletion"