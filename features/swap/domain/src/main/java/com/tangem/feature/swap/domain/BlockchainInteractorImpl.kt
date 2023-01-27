package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.lib.crypto.TransactionManager
import javax.inject.Inject

internal class BlockchainInteractorImpl @Inject constructor(
    private val transactionManager: TransactionManager,
) : BlockchainInteractor {

    override fun getBlockchainId(networkId: String): String {
        return transactionManager.getBlockchainId(networkId)
    }

    override fun getTokenDecimals(token: Currency): Int {
        return if (token is Currency.NonNativeToken) {
            token.decimalCount
        } else {
            transactionManager.getNativeTokenDecimals(token.networkId)
        }
    }
}