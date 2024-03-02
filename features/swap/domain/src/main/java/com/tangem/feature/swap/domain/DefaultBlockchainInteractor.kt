package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.domain.NetworkInfo
import com.tangem.lib.crypto.TransactionManager
import javax.inject.Inject

internal class DefaultBlockchainInteractor @Inject constructor(
    private val transactionManager: TransactionManager,
) : BlockchainInteractor {

    override fun getBlockchainInfo(networkId: String): NetworkInfo {
        return transactionManager.getBlockchainInfo(networkId).let {
            NetworkInfo(
                name = it.name,
                blockchainId = it.blockchainId,
            )
        }
    }

    override fun getExplorerTransactionLink(networkId: String, txHash: String): String {
        return transactionManager.getExplorerTransactionLink(networkId, txHash)
    }
}