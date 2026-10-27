package com.tangem.features.tangempay.withdraw.bank.preview.model

import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.toNewBankAccountDetails
import java.math.BigDecimal
import java.math.RoundingMode

/** The create-order payload, or null while the draft misses the amount, the source, the rail or the recipient. */
internal fun BankWithdrawDraft.toOrderRequest(): BankOfframpOrderRequest? {
    val amount = amount ?: return null
    val source = source ?: return null
    val destination = destination ?: return null
    val recipient = toRecipient() ?: return null
    return BankOfframpOrderRequest(
        amountInCents = amount.truncatedToCents().movePointRight(CENTS_SCALE).longValueExact(),
        chainId = source.chainId,
        tokenContractAddress = source.token.contractAddress,
        tokenDecimals = source.token.decimals,
        rail = destination.rail,
        recipient = recipient,
    )
}

internal fun BankWithdrawDraft.toRecipient(): BankOfframpRecipient? =
    recipient?.let { BankOfframpRecipient.Saved(bankAccountId = it.id) }
        ?: toNewBankAccountDetails()?.let(BankOfframpRecipient::New)

/** Truncated, not rounded: the balance check runs on the raw amount, so rounding up could overcharge. */
internal fun BigDecimal.truncatedToCents(): BigDecimal = setScale(CENTS_SCALE, RoundingMode.DOWN)

private const val CENTS_SCALE = 2