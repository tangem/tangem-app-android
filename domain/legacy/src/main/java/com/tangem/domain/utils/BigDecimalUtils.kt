package com.tangem.domain.utils

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import java.math.BigDecimal
import com.tangem.blockchain.common.Amount as SdkAmount

/** Converts `BigDecimal` [cryptoCurrencyStatus] to [SdkAmount] */
fun BigDecimal.convertToSdkAmount(cryptoCurrencyStatus: CryptoCurrencyStatus): SdkAmount {
    val cryptoCurrency = cryptoCurrencyStatus.currency
    val yieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
    return SdkAmount(
        currencySymbol = cryptoCurrency.symbol,
        value = this,
        decimals = cryptoCurrency.decimals,
        type = when (cryptoCurrency) {
            is CryptoCurrency.Coin -> AmountType.Coin
            is CryptoCurrency.Token -> {
                val token = Token(
                    symbol = cryptoCurrency.symbol,
                    contractAddress = cryptoCurrency.contractAddress,
                    decimals = cryptoCurrency.decimals,
                )
                if (yieldSupplyStatus == null) {
                    AmountType.Token(token = token)
                } else {
                    AmountType.TokenYieldSupply(
                        token = token,
                        isActive = yieldSupplyStatus.isActive,
                        isInitialized = yieldSupplyStatus.isInitialized,
                        isAllowedToSpend = yieldSupplyStatus.isAllowedToSpend,
                    )
                }
            }
        },
    )
}