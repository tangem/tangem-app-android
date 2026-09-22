package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Polls the order until the backend asks for a card signature and returns the payload hash to sign. */
class AwaitBankOfframpSignatureUseCase(
    private val repository: BankOfframpOrderRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        orderId: String,
        timeout: Duration = DEFAULT_TIMEOUT,
    ): Either<BankOfframpError, String> {
        return withTimeoutOrNull(timeout) { pollUntilSignatureRequired(userWalletId, orderId) }
            ?: BankOfframpError.PollingTimeout.left()
    }

    private suspend fun pollUntilSignatureRequired(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<BankOfframpError, String> {
        while (true) {
            val state = repository.getOrderState(userWalletId, orderId).getOrElse { return it.left() }
            when (state) {
                is BankOfframpOrderState.AwaitingSignature -> return state.payloadHash.right()
                is BankOfframpOrderState.Processing -> delay(POLL_INTERVAL)
                is BankOfframpOrderState.Failed -> return state.error.left()
                BankOfframpOrderState.Completed,
                BankOfframpOrderState.Canceled,
                -> return BankOfframpError.Unknown(
                    cause = IllegalStateException("Order $orderId reached $state before signature"),
                ).left()
            }
        }
    }

    private companion object {
        val POLL_INTERVAL = 3.seconds
        val DEFAULT_TIMEOUT = 60.seconds
    }
}