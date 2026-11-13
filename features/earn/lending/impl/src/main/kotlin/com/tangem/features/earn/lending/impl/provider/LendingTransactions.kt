package com.tangem.features.earn.lending.impl.provider

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import java.math.BigDecimal

/** The provider's transactions, so the product knows nothing of the provider */
internal interface LendingTransactions {

    /** Preparation (if any) first, the deposit last */
    suspend fun deposit(
        userWalletId: UserWalletId,
        asset: LendingAsset,
        amount: BigDecimal,
        state: LendingProviderState,
    ): Either<LendingTxError, List<TransactionData.Uncompiled>>

    suspend fun withdraw(
        userWalletId: UserWalletId,
        asset: LendingAsset,
        amount: BigDecimal,
        position: BigDecimal,
        state: LendingProviderState,
    ): Either<LendingTxError, TransactionData.Uncompiled>

    /** Same list with the estimated gas of each transaction at the normal price */
    suspend fun estimate(
        userWallet: UserWallet,
        asset: LendingAsset,
        txs: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, List<TransactionData.Uncompiled>>

    /** Price levels for the summed estimated gas limit of [txs], for the fee selector */
    suspend fun feeLevels(
        userWallet: UserWallet,
        asset: LendingAsset,
        txs: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, TransactionFee>

    suspend fun verify(
        userWalletId: UserWalletId,
        asset: LendingAsset,
        txs: List<TransactionData.Uncompiled>,
        expected: LendingTxExpectation,
        state: LendingProviderState,
    ): Either<LendingTxError, Unit>
}

internal sealed interface LendingTxExpectation {

    val owner: String

    data class Deposit(override val owner: String, val amount: BigDecimal) : LendingTxExpectation

    data class Withdraw(
        override val owner: String,
        val amount: BigDecimal,
        val isEntire: Boolean,
    ) : LendingTxExpectation
}

internal sealed interface LendingTxError {

    data object UpdateRequired : LendingTxError

    data object ReadFailed : LendingTxError

    data class Unverified(val reason: String) : LendingTxError
}