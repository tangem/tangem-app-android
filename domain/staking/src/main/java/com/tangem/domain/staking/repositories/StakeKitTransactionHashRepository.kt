package com.tangem.domain.staking.repositories

import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata

interface StakeKitTransactionHashRepository {

    suspend fun submitHash(transactionId: String, transactionHash: String)

    suspend fun storeUnsubmittedHash(unsubmittedTransactionMetadata: UnsubmittedTransactionMetadata)

    suspend fun sendUnsubmittedHashes()
}