package com.tangem.domain.tokens.utils

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigDecimal

/** Converts `BigDecimal` [cryptoCurrency] to [Amount] */
fun BigDecimal.convertToAmount(cryptoCurrency: CryptoCurrency) = Amount(
    currencySymbol = cryptoCurrency.symbol,
    value = this,
    decimals = cryptoCurrency.decimals,
    type = when (cryptoCurrency) {
        is CryptoCurrency.Coin -> AmountType.Coin
        is CryptoCurrency.Token -> AmountType.Token(
            token = Token(
                symbol = cryptoCurrency.symbol,
                contractAddress = cryptoCurrency.contractAddress,
                decimals = cryptoCurrency.decimals,
            ),
        )
    },
)