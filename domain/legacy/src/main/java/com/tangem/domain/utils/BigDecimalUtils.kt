package com.tangem.domain.utils

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal
import com.tangem.blockchain.common.Amount as SdkAmount

/** Converts `BigDecimal` [cryptoCurrency] to [SdkAmount] */
fun BigDecimal.convertToSdkAmount(
    cryptoCurrency: CryptoCurrency,
    amountType: AmountType = getAmountTypeFromCryptoCurrency(cryptoCurrency),
): SdkAmount = SdkAmount(
    currencySymbol = cryptoCurrency.symbol,
    value = this,
    decimals = cryptoCurrency.decimals,
    type = amountType,
)

/**
 * Converts [CryptoCurrency] to [AmountType] based on its type
 */
private fun getAmountTypeFromCryptoCurrency(cryptoCurrency: CryptoCurrency) = when (cryptoCurrency) {
    is CryptoCurrency.Coin -> AmountType.Coin
    is CryptoCurrency.Token -> AmountType.Token(
        token = Token(
            symbol = cryptoCurrency.symbol,
            contractAddress = cryptoCurrency.contractAddress,
            decimals = cryptoCurrency.decimals,
        ),
    )
}