package com.tangem.data.tokens.utils

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import timber.log.Timber
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
        val formattedAmounts = hashMapOf<CryptoCurrency.ID, BigDecimal>()

        currencies.forEach { currency ->
            val amount = when (currency) {
                is CryptoCurrency.Coin -> amounts.singleOrNull { it is CryptoCurrencyAmount.Coin }
                is CryptoCurrency.Token -> amounts.singleOrNull {
                    it is CryptoCurrencyAmount.Token &&
                        it.id == getTokenIdString(currency.id) &&
                        it.tokenContractAddress == currency.contractAddress
                }
            }?.value

            if (amount == null) {
                Timber.e("Unable to find a token amount for: ${currency.name}")
            } else {
                formattedAmounts[currency.id] = amount
            }
        }

        return formattedAmounts
    }
}