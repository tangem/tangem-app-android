package com.tangem.features.tangempay.withdraw.bank.preview.model

import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.toNewBankAccountDetails
import java.math.RoundingMode

/** The create-order payload, or null while the draft misses the amount, the source, the rail or the recipient. */
internal fun BankWithdrawDraft.toOrderRequest(): BankOfframpOrderRequest? {
    val amount = amount ?: return null
    val source = source ?: return null
    val destination = destination ?: return null
    val recipient = recipient?.let { BankOfframpRecipient.Saved(bankAccountId = it.id) }
        ?: toNewBankAccountDetails()?.let(BankOfframpRecipient::New)
        ?: return null
    return BankOfframpOrderRequest(
        // Truncated, not rounded: the balance check runs on the raw amount, so rounding up could overcharge.
        amountInCents = amount.movePointRight(CENTS_SCALE).setScale(0, RoundingMode.DOWN).longValueExact(),
        chainId = source.chainId,
        tokenContractAddress = source.token.contractAddress,
        rail = destination.rail,
        recipient = recipient,
    )
}

private const val CENTS_SCALE = 2