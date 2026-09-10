package com.tangem.domain.pay.model.bankofframp

import com.tangem.domain.pay.model.OrderStep

data class BankOfframpOrderRequest(
    val amountInCents: Long,
    val chainId: Long,
    val tokenContractAddress: String,
    val rail: BankRail,
    val recipient: BankOfframpRecipient,
)

sealed interface BankOfframpRecipient {
    data class Saved(val bankAccountId: String) : BankOfframpRecipient
    data class New(val details: BankAccountDetails) : BankOfframpRecipient
}

/** [bankAccountId] is null when the backend does not return the id of a recipient created by the order. */
data class BankOfframpOrderResult(
    val orderId: String,
    val bankAccountId: String?,
)

sealed interface BankOfframpOrderState {
    data class Processing(val step: OrderStep) : BankOfframpOrderState
    data class AwaitingSignature(val payloadHash: String) : BankOfframpOrderState
    data object Completed : BankOfframpOrderState
    data object Canceled : BankOfframpOrderState
    data class Failed(val error: BankOfframpError) : BankOfframpOrderState
}

sealed interface BankOfframpSignResult {
    data object Signed : BankOfframpSignResult
    data object Cancelled : BankOfframpSignResult
}