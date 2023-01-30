package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.NetworkInfo
import com.tangem.lib.crypto.TransactionManager
import javax.inject.Inject

internal class BlockchainInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
) : BlockchainInteractor {

    override fun getBlockchainInfo(networkId: String): NetworkInfo {
        return transactionManager.getBlockchainInfo(networkId).let {
            NetworkInfo(
                name = it.name,
                blockchainId = it.blockchainId,
                blockchainCurrency = it.blockchainCurrency,
            )
        }
    }

    override fun getExplorerTransactionLink(networkId: String, txAddress: String): String {
        return transactionManager.getExplorerTransactionLink(networkId, txAddress)
    }

    override fun getTokenDecimals(token: Currency): Int {
        return if (token is Currency.NonNativeToken) {
            token.decimalCount
        } else {
            transactionManager.getNativeTokenDecimals(token.networkId)
        }
    }
}
