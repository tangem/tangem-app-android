package com.tangem.features.earn.flow.impl

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.flow.api.EarnDepositCapacity
import com.tangem.features.earn.flow.impl.model.EarnFlowStatuses
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.EarnFeeSelection
import com.tangem.features.earn.product.api.session.EarnFlowAction
import com.tangem.utils.logging.TangemLogger
import java.math.BigDecimal
import javax.inject.Inject

internal class DefaultEarnDepositCapacity @Inject constructor(
    private val sessionFactories: EarnSessionFactories,
    private val statuses: EarnFlowStatuses,
) : EarnDepositCapacity {

    override suspend fun availableMax(
        productId: EarnProductId,
        params: EarnProduct.Params,
        action: EarnFlowAction,
        balance: BigDecimal,
    ): Either<EarnDepositCapacity.Unavailable, BigDecimal> = either {
        val factory = ensureNotNull(sessionFactories.find(productId)) { unavailable(productId, reason = "no session") }
        val session = factory.create(params)
        // Without an amount the session prices the whole source, as the deposit screen does before any input
        val fee = session.loadFee(
            action = action,
            amount = null,
        ).mapLeft { unavailable(productId, reason = it) }.bind().normal
        val feeValue = ensureNotNull(fee.amount.value) { unavailable(productId, reason = "no fee amount") }
        val isSubtractAvailable = statuses.isAmountSubtractAvailable(
            product = params,
            selection = EarnFeeSelection(fee = fee, extended = null),
        )
        val deduction = if (isSubtractAvailable) feeValue else BigDecimal.ZERO
        (balance - deduction).coerceAtLeast(BigDecimal.ZERO)
    }

    private fun unavailable(productId: EarnProductId, reason: Any): EarnDepositCapacity.Unavailable {
        TangemLogger.e("Earn deposit capacity: ${productId.value} failed: $reason")
        return EarnDepositCapacity.Unavailable
    }
}