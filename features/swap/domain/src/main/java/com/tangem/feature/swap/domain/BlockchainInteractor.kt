package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.domain.NetworkInfo

interface BlockchainInteractor {

    /**
     * In app blockchain id, actual in blockchain sdk, not the same as networkId
     *
     * workaround till not use backend only and not integrated server vs sdk
     */
    fun getBlockchainInfo(networkId: String): NetworkInfo

    fun getExplorerTransactionLink(networkId: String, txHash: String): String
}