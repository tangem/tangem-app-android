package com.tangem.features.earn.product.api.session

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import java.math.BigDecimal

sealed interface EarnSigningStep {

    data class AwaitingConfirmation(val stepLabel: TextReference?, val tx: EarnTxAnalytics) : EarnSigningStep

    data class Sent(val ref: EarnTxRef) : EarnSigningStep

    data class Done(val ref: EarnTxRef, val result: EarnResult) : EarnSigningStep

    /**
     * Signing or sending failed. [SendTransactionError.UserCancelledError] is a refusal on the card: no error dialog,
     * the flow goes back to the confirmation.
     */
    data class Failed(val error: SendTransactionError) : EarnSigningStep
}

/** [status] is the existing [TxInfo.TransactionStatus]; what only the product knows on top of it goes to [detail]. */
data class EarnTxStatus(
    val status: TxInfo.TransactionStatus,
    val detail: EarnStatusDetail? = null,
)

sealed interface EarnStatusDetail {

    data class Received(val amount: BigDecimal) : EarnStatusDetail

    data class AwaitingSettlement(val expectedBy: kotlinx.datetime.Instant?) : EarnStatusDetail

    data object Returned : EarnStatusDetail

    data class ErrorCode(val code: String) : EarnStatusDetail
}