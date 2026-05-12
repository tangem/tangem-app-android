package com.tangem.domain.transaction

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

fun CryptoCurrency.toBlockchainAmount(value: BigDecimal): Amount = Amount(
    currencySymbol = symbol,
    value = value,
    decimals = decimals,
    type = when (this) {
        is CryptoCurrency.Coin -> AmountType.Coin
        is CryptoCurrency.Token -> AmountType.Token(
            token = Token(
                symbol = symbol,
                contractAddress = contractAddress,
                decimals = decimals,
            ),
        )
    },
)