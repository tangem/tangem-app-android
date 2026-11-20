package com.tangem.features.earn.flow.api

import arrow.core.Either
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.EarnFlowAction
import java.math.BigDecimal

/** The Max the deposit screen of the shared flow would offer for a wallet [balance], after the same fee deduction. */
interface EarnDepositCapacity {

    suspend fun availableMax(
        productId: EarnProductId,
        params: EarnProduct.Params,
        action: EarnFlowAction,
        balance: BigDecimal,
    ): Either<Unavailable, BigDecimal>

    data object Unavailable
}