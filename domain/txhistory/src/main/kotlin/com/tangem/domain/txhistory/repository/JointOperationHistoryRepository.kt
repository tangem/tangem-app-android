package com.tangem.domain.txhistory.repository

import arrow.core.Either
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationRecord
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

    /** The locally synced operation [operationId] in any status; `null` while it has not been synced */
    fun observeOperation(userWalletId: UserWalletId, operationId: String): Flow<JointOperationRecord?>

    /**
     * Fetches the operation [operationId] and stores it. [eTag] is the tag of the previous [FetchResult.Updated];
     * `Left` is a failed request, after which the stored operation stays as it was.
     */
    suspend fun fetchOperation(
        userWalletId: UserWalletId,
        operationId: String,
        eTag: String?,
    ): Either<Throwable, FetchResult>

    sealed interface FetchResult {

        /** The operation is stored; [eTag] is the tag to send with the next fetch, `null` when the backend sent none */
        data class Updated(val eTag: String?) : FetchResult

        /** The operation has not changed since the given tag */
        data object NotModified : FetchResult

        /** The backend does not know the operation or refuses its id, or the wallet lost its slot in the account */
        data object Gone : FetchResult
    }
}