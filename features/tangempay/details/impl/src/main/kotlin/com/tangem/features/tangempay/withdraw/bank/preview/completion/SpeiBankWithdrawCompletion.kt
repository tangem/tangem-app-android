package com.tangem.features.tangempay.withdraw.bank.preview.completion

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpTransfer
import com.tangem.domain.tangempay.repository.BankOfframpOrderRepository
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransfer
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransferError
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.isLockTrusted
import com.tangem.features.tangempay.withdraw.bank.preview.model.toFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.toRecipient
import com.tangem.features.tangempay.withdraw.bank.preview.model.truncatedToCents
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.time.CurrentTimeProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.util.UUID

internal class SpeiBankWithdrawCompletion @AssistedInject constructor(
    @Assisted private val userWalletId: UserWalletId,
    @Assisted private val productInstanceId: String,
    @Assisted private val onNetworkFeeChanged: (BigDecimal) -> Unit,
    private val bankOfframpOrderRepository: BankOfframpOrderRepository,
    private val moneyAccountTransfer: MoneyAccountTransfer,
    private val currentTimeProvider: CurrentTimeProvider,
) : BankWithdrawCompletion {

    override val quote: StateFlow<BankOfframpQuote?>
        field = MutableStateFlow<BankOfframpQuote?>(null)

    private var attempt: QuoteAttempt? = null

    private val registeredRecipients = mutableMapOf<BankOfframpRecipient, BankOfframpRecipient.Saved>()

    private var isTransferOutcomeUnknown = false

    override suspend fun prepare(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {
        val request = draft.toQuoteRequest() ?: return
        if (sendableQuoteFor(request) == null) refreshQuote(request = request, onState = onState)
    }

    override suspend fun complete(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {
        if (isTransferOutcomeUnknown) {
            onState(CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown))
            return
        }
        val request = draft.toQuoteRequest() ?: return
        val token = draft.source?.token ?: return
        val quote = sendableQuoteFor(request)
        if (quote == null) {
            refreshQuote(request = request, onState = onState)
            return
        }
        onState(CompletionState.Sending)
        bankOfframpOrderRepository.createTransfer(userWalletId = userWalletId, quoteId = quote.quoteId).fold(
            ifLeft = { error -> onTransferRequestFailed(error = error, request = request, onState = onState) },
            ifRight = { transfer ->
                if (transfer.matches(quote = quote, amount = request.amount)) {
                    send(transfer = transfer, token = token, maxNetworkFee = draft.networkFee, onState = onState)
                } else {
                    onTransferMismatch(transfer = transfer, quote = quote, onState = onState)
                }
            },
        )
    }

    private fun onTransferMismatch(
        transfer: BankOfframpTransfer,
        quote: BankOfframpQuote,
        onState: (CompletionState) -> Unit,
    ) {
        TangemLogger.withTag(TAG).e(
            "SPEI transfer ${transfer.transferId} asks for ${transfer.sourceAmount}, quote ${quote.quoteId} " +
                "for ${quote.sourceAmount}; nothing sent",
        )
        attempt = null
        onState(CompletionState.Failed(PreviewFailure.General))
    }

    private fun sendableQuoteFor(request: QuoteRequest): BankOfframpQuote? {
        val current = attempt?.takeIf { it.request == request } ?: return null
        return current.quote?.takeIf {
            !current.isLockTrusted || currentTimeProvider.nowMillis() < it.sendDeadline.millis
        }
    }

    /**
     * A quote request the backend never answered (offline, 5xx) keeps the terms loading and is asked again after a
     * growing pause, under the same key, for about two minutes. A refusal, an expired session or a longer silence shows
     * its failure, since the same code also covers failures that never recover.
     */
    private suspend fun refreshQuote(request: QuoteRequest, onState: (CompletionState) -> Unit) {
        onState(CompletionState.Idle)
        var retryDelayMillis = FIRST_RETRY_DELAY_MILLIS
        var attemptsLeft = MAX_UNANSWERED_QUOTE_ATTEMPTS
        while (true) {
            val error = requestQuote(request).leftOrNull() ?: return
            attemptsLeft--
            if (!error.isUnanswered() || attemptsLeft == 0) {
                onQuoteFailed(error = error, onState = onState)
                return
            }
            val message = "SPEI quote failed, retrying in $retryDelayMillis ms: $error"
            // Only the first miss goes to the exported log; an outage would otherwise add a line every 30 s.
            if (retryDelayMillis == FIRST_RETRY_DELAY_MILLIS) {
                TangemLogger.withTag(TAG).e(message)
            } else {
                TangemLogger.withTag(TAG).d(message)
            }
            delay(retryDelayMillis)
            retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(MAX_RETRY_DELAY_MILLIS)
        }
    }

    private suspend fun requestQuote(request: QuoteRequest): Either<BankOfframpError, BankOfframpQuote> {
        val current = attempt?.takeIf { it.request == request && it.quote == null }
            ?: QuoteAttempt(request = request, idempotencyKey = UUID.randomUUID().toString())
        attempt = current
        quote.value = null
        return bankOfframpOrderRepository.createQuote(
            userWalletId = userWalletId,
            productInstanceId = productInstanceId,
            amount = request.amount,
            recipient = registeredRecipients[request.recipient] ?: request.recipient,
            idempotencyKey = current.idempotencyKey,
        )
            .onRight { newQuote ->
                attempt = current.copy(
                    quote = newQuote,
                    isLockTrusted = newQuote.isLockTrusted(currentTimeProvider.nowMillis()),
                )
                if (request.recipient is BankOfframpRecipient.New) {
                    registeredRecipients[request.recipient] = BankOfframpRecipient.Saved(newQuote.bankAccountId)
                }
                quote.value = newQuote
            }
            .onLeft { error -> if (!error.isOutcomeUnknown()) attempt = null }
    }

    private suspend fun onTransferRequestFailed(
        error: BankOfframpError,
        request: QuoteRequest,
        onState: (CompletionState) -> Unit,
    ) {
        TangemLogger.withTag(TAG).e("SPEI transfer creation failed: $error")
        if (error == BankOfframpError.QuoteExpired) {
            attempt = null
            refreshQuote(request = request, onState = onState)
            return
        }
        if (!error.isOutcomeUnknown()) attempt = null
        onState(CompletionState.Failed(failure = error.toFailure(), errorCode = error.errorCode))
    }

    private fun onQuoteFailed(error: BankOfframpError, onState: (CompletionState) -> Unit) {
        TangemLogger.withTag(TAG).e("SPEI quote failed: $error")
        onState(CompletionState.Failed(failure = error.toFailure(), errorCode = error.errorCode))
    }

    private suspend fun send(
        transfer: BankOfframpTransfer,
        token: CryptoCurrency.Token,
        maxNetworkFee: BigDecimal,
        onState: (CompletionState) -> Unit,
    ) {
        moneyAccountTransfer.send(
            userWalletId = userWalletId,
            token = token,
            amount = transfer.sourceAmount,
            destination = transfer.depositAddress,
            maxNetworkFee = maxNetworkFee,
        ).fold(
            ifLeft = { error -> onSendFailed(error = error, onState = onState) },
            ifRight = {
                attempt = null
                onState(CompletionState.Completed)
            },
        )
    }

    private fun onSendFailed(error: MoneyAccountTransferError, onState: (CompletionState) -> Unit) {
        when (error) {
            MoneyAccountTransferError.Cancelled -> onState(CompletionState.Idle)
            MoneyAccountTransferError.NotSent -> onState(CompletionState.Failed(PreviewFailure.General))
            MoneyAccountTransferError.OutcomeUnknown -> {
                isTransferOutcomeUnknown = true
                onState(CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown))
            }
            is MoneyAccountTransferError.NetworkFeeIncreased -> {
                onNetworkFeeChanged(error.networkFee)
                onState(CompletionState.Idle)
            }
        }
    }

    private fun BankWithdrawDraft.toQuoteRequest(): QuoteRequest? {
        val amount = amount ?: return null
        val recipient = toRecipient() ?: return null
        return QuoteRequest(amount = amount.truncatedToCents(), recipient = recipient)
    }

    private fun BankOfframpError.isOutcomeUnknown(): Boolean = this is BankOfframpError.Unknown && isOutcomeUnknown

    private fun BankOfframpError.isUnanswered(): Boolean =
        this is BankOfframpError.Unknown && errorCode in UNANSWERED_ERROR_CODES

    private fun BankOfframpTransfer.matches(quote: BankOfframpQuote, amount: BigDecimal): Boolean =
        sourceAmount.compareTo(quote.sourceAmount) == 0 && quote.sourceAmount.compareTo(amount) == 0

    private data class QuoteRequest(val amount: BigDecimal, val recipient: BankOfframpRecipient)

    private data class QuoteAttempt(
        val request: QuoteRequest,
        val idempotencyKey: String,
        val quote: BankOfframpQuote? = null,
        val isLockTrusted: Boolean = true,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            userWalletId: UserWalletId,
            productInstanceId: String,
            onNetworkFeeChanged: (BigDecimal) -> Unit,
        ): SpeiBankWithdrawCompletion
    }
}

private const val TAG = "SpeiBankWithdrawCompletion"
private const val FIRST_RETRY_DELAY_MILLIS = 2_000L
private const val MAX_RETRY_DELAY_MILLIS = 30_000L
private const val MAX_UNANSWERED_QUOTE_ATTEMPTS = 8
private val UNANSWERED_ERROR_CODES = setOf(
    TangemPayApiError.UnknownWithoutCode.errorCode,
    TangemPayApiError.ServerUnavailable.errorCode,
)