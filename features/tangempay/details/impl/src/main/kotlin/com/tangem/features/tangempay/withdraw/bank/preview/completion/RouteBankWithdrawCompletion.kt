package com.tangem.features.tangempay.withdraw.bank.preview.completion

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankOfframpRoute
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransfer
import com.tangem.features.tangempay.withdraw.bank.model.MoneyAccountTransferError
import com.tangem.features.tangempay.withdraw.bank.preview.model.PreviewFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.toFailure
import com.tangem.features.tangempay.withdraw.bank.preview.model.toRecipient
import com.tangem.features.tangempay.withdraw.bank.preview.model.truncatedToCents
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.math.BigDecimal
import java.util.UUID

internal class RouteBankWithdrawCompletion @AssistedInject constructor(
    @Assisted private val userWalletId: UserWalletId,
    @Assisted private val productInstanceId: String,
    @Assisted private val onDestinationChanged: (OfframpDestination) -> Unit,
    private val bankOfframpOrderRepository: BankOfframpOrderRepository,
    private val moneyAccountTransfer: MoneyAccountTransfer,
) : BankWithdrawCompletion {

    private var attempt: RouteAttempt? = null

    private var isTransferOutcomeUnknown = false

    override suspend fun complete(draft: BankWithdrawDraft, onState: (CompletionState) -> Unit) {
        if (isTransferOutcomeUnknown) {
            onState(CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown))
            return
        }
        val amount = draft.amount ?: return
        val token = draft.source?.token ?: return
        val destination = draft.destination ?: return
        val recipient = draft.toRecipient() ?: return
        onState(CompletionState.Sending)
        val route = createRoute(rail = destination.rail, recipient = recipient, onState = onState) ?: return
        if (!route.matches(destination)) {
            onDestinationChanged(destination.copy(fee = route.fee, eta = route.eta))
            onState(CompletionState.Idle)
            return
        }
        transfer(route = route, amount = amount.truncatedToCents() + route.fee.amount, token = token, onState = onState)
    }

    private suspend fun createRoute(
        rail: BankRail,
        recipient: BankOfframpRecipient,
        onState: (CompletionState) -> Unit,
    ): BankOfframpRoute? {
        val current = attempt?.takeIf { it.rail == rail && it.recipient == recipient }
            ?: RouteAttempt(rail = rail, recipient = recipient, idempotencyKey = newIdempotencyKey())
        attempt = current
        current.route?.let { return it }
        return bankOfframpOrderRepository.createRoute(
            userWalletId = userWalletId,
            productInstanceId = productInstanceId,
            rail = current.rail,
            recipient = current.recipient,
            idempotencyKey = current.idempotencyKey,
        ).fold(
            ifLeft = { error ->
                onRouteFailed(error = error, onState = onState)
                null
            },
            ifRight = { route ->
                attempt = current.copy(route = route)
                route
            },
        )
    }

    private fun onRouteFailed(error: BankOfframpError, onState: (CompletionState) -> Unit) {
        TangemLogger.withTag(TAG).e("Bank withdraw route creation failed: $error")
        val isOutcomeUnknown = error is BankOfframpError.Unknown && error.isOutcomeUnknown
        if (!isOutcomeUnknown) attempt = null
        if (error == BankOfframpError.Validation) {
            onState(CompletionState.Idle)
        } else {
            onState(CompletionState.Failed(error.toFailure()))
        }
    }

    private suspend fun transfer(
        route: BankOfframpRoute,
        amount: BigDecimal,
        token: CryptoCurrency.Token,
        onState: (CompletionState) -> Unit,
    ) {
        moneyAccountTransfer.send(
            userWalletId = userWalletId,
            token = token,
            amount = amount,
            destination = route.depositAddress,
        ).fold(
            ifLeft = { error -> onTransferFailed(error = error, onState = onState) },
            ifRight = {
                attempt = null
                onState(CompletionState.Completed)
            },
        )
    }

    private fun onTransferFailed(error: MoneyAccountTransferError, onState: (CompletionState) -> Unit) {
        when (error) {
            MoneyAccountTransferError.Cancelled -> onState(CompletionState.Idle)
            MoneyAccountTransferError.NotSent -> onState(CompletionState.Failed(PreviewFailure.General))
            MoneyAccountTransferError.OutcomeUnknown -> {
                isTransferOutcomeUnknown = true
                onState(CompletionState.Failed(PreviewFailure.TransferOutcomeUnknown))
            }
        }
    }

    private fun newIdempotencyKey(): String = UUID.randomUUID().toString()

    private fun BankOfframpRoute.matches(destination: OfframpDestination): Boolean =
        fee.amount.compareTo(destination.fee.amount) == 0 &&
            fee.currency.equals(destination.fee.currency, ignoreCase = true) &&
            eta == destination.eta

    private data class RouteAttempt(
        val rail: BankRail,
        val recipient: BankOfframpRecipient,
        val idempotencyKey: String,
        val route: BankOfframpRoute? = null,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            userWalletId: UserWalletId,
            productInstanceId: String,
            onDestinationChanged: (OfframpDestination) -> Unit,
        ): RouteBankWithdrawCompletion
    }
}

private const val TAG = "RouteBankWithdrawCompletion"