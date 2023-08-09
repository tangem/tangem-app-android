package com.tangem.data.tokens.utils

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import java.math.BigDecimal

internal class NetworkStatusFactory {

    fun createNetworkStatus(
        networkId: Network.ID,
        result: UpdateWalletManagerResult,
        currencies: Set<CryptoCurrency>,
    ): NetworkStatus {
        return NetworkStatus(
            networkId = networkId,
            value = when (result) {
                is UpdateWalletManagerResult.MissedDerivation -> NetworkStatus.MissedDerivation
                is UpdateWalletManagerResult.Unreachable -> NetworkStatus.Unreachable
                is UpdateWalletManagerResult.NoAccount -> NetworkStatus.NoAccount(result.amountToCreateAccount)
                is UpdateWalletManagerResult.Verified -> NetworkStatus.Verified(
                    amounts = formatAmounts(result.tokensAmounts, currencies),
                    hasTransactionsInProgress = result.hasTransactionsInProgress,
                )
            },
        )
    }

    private fun formatAmounts(
        amounts: Set<CryptoCurrencyAmount>,
        currencies: Set<CryptoCurrency>,
    ): Map<CryptoCurrency.ID, BigDecimal> {
        return amounts
            .asSequence()
            .mapNotNull { amount ->
                val currency = when (amount) {
                    is CryptoCurrencyAmount.Coin -> currencies.singleOrNull { it is CryptoCurrency.Coin }
                    is CryptoCurrencyAmount.Token -> currencies.firstOrNull {
                        it is CryptoCurrency.Token &&
                            getTokenIdString(it.id) == amount.id &&
                            it.contractAddress == amount.tokenContractAddress
                    }
                }

                currency?.id?.let { it to amount.value }
            }
            .toMap()
    }
}
