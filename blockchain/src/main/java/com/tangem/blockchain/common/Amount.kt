package com.tangem.blockchain.common

import java.math.BigDecimal

data class Amount(
        val currencySymbol: String,
        var value: BigDecimal? = null,
        val address: String? = null,
        val decimals: Int,
        val type: AmountType = AmountType.Coin
) {
    constructor(
            value: BigDecimal?,
            blockchain: Blockchain,
            address: String? = null,
            type: AmountType = AmountType.Coin
    ) : this(blockchain.currency, value, address, blockchain.decimals(), type)

    constructor(token: Token, value: BigDecimal? = null) :
            this(token.symbol, value, token.contractAddress, token.decimals, AmountType.Token)

    constructor(amount: Amount, value: BigDecimal) :
            this(amount.currencySymbol, value, amount.address, amount.decimals, amount.type)
}

enum class AmountType { Coin, Token, Reserve }