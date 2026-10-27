package com.tangem.domain.jointaccount.gas

import com.tangem.utils.extensions.addHexPrefix
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

fun Account.Joint.gasTokenQuoteId(network: Network, gasToken: String): CryptoCurrency.RawID? {
    val address = gasToken.lowercase().addHexPrefix()

    return cryptoCurrencies
        .filterIsInstance<CryptoCurrency.Token>()
        .firstOrNull { it.network == network && it.contractAddress.lowercase().addHexPrefix() == address }
        ?.id
        ?.rawCurrencyId
}