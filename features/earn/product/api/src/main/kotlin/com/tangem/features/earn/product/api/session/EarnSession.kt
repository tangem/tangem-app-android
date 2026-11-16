package com.tangem.features.earn.product.api.session

import arrow.core.Either
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.features.earn.product.api.product.EarnProduct
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * Transaction logic of a product behind the shared flow, without UI. The action arrives as a parameter and there is no
 * list of actions: an action the session cannot build is rejected, never thrown. That is
 * `Left(EarnSessionError.Failed())` from [maxAmount] and [prepare], `Left(GetFeeError.UnknownError)` from [loadFee]
 * and [loadFeeExtended].
 */
interface EarnSession {

    suspend fun loadFee(action: EarnFlowAction, amount: EarnAmount?): Either<GetFeeError, TransactionFee>

    suspend fun loadFeeExtended(
        action: EarnFlowAction,
        amount: EarnAmount?,
        feeCurrency: CryptoCurrencyStatus?,
    ): Either<GetFeeError, TransactionFeeExtended>

    suspend fun maxAmount(action: EarnFlowAction): Either<EarnSessionError, BigDecimal>

    suspend fun prepare(
        action: EarnFlowAction,
        amount: EarnAmount?,
        fee: EarnFeeSelection,
    ): Either<EarnSessionError, EarnPlan>

    suspend fun verify(plan: EarnPlan): EarnVerification

    fun execute(plan: VerifiedPlan): Flow<EarnSigningStep>

    fun track(ref: EarnTxRef): Flow<EarnTxStatus>

    fun interface Factory {

        fun create(params: EarnProduct.Params): EarnSession
    }
}