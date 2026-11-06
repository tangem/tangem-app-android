package com.tangem.domain.txhistory.repository

import arrow.core.Either
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationFetchResult
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/** Locally synced history of the joint account operations of a wallet */
interface JointOperationHistoryRepository {

    /**
     * Finished operations of the joint account [accountId] in the network [chainId] that finished no earlier than
     * [fromFinishedAtMillis], newest first.
     */
    fun getFinishedOperations(
        accountId: AccountId,
        chainId: Long,
        fromFinishedAtMillis: Long,
    ): Flow<List<FinishedJointOperation>>

    /** Ids of every finished operation of the wallet's joint accounts */
    fun getFinishedOperationIds(userWalletId: UserWalletId): Flow<Set<String>>

    fun observeOperation(userWalletId: UserWalletId, operationId: String): Flow<FinishedJointOperation?>

    suspend fun fetchOperation(
        userWalletId: UserWalletId,
        operationId: String,
    ): Either<Throwable, JointOperationFetchResult>
}