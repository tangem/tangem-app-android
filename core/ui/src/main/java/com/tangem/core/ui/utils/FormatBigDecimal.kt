package com.tangem.core.ui.utils

import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.core.ui.format.bigdecimal.asDefaultAmount
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrency.ID
import com.tangem.domain.tokens.model.Network
import java.math.BigDecimal

// TEST INTERFACE

val network1 = Network(
    id = Network.ID("network1"),
    name = "Network One",
    isTestnet = false,
    standardType = Network.StandardType.ERC20,
    backendId = "network1",
    currencySymbol = "ETH",
    derivationPath = Network.DerivationPath.None,
    hasFiatFeeRate = true,
    canHandleTokens = true,
)

val cryptoCurrencyMock = CryptoCurrency.Token(
    id = ID(
        ID.Prefix.TOKEN_PREFIX,
        ID.Body.NetworkId(network1.id.value),
        ID.Suffix.RawID("token3"),
    ),
    network = network1,
    name = "Token 3",
    symbol = "T3",
    isCustom = false,
    decimals = 8,
    iconUrl = null,
    contractAddress = "address",
)

fun foobar() {
    val format = BigDecimalCryptoFormat(
        symbol = "BTC",
        decimals = 12,
    )

    val koinPrice = BigDecimal.TEN

    koinPrice.formatAs {
        crypto(cryptoCurrency = "BTC", decimals = 12).asDefaultAmount()
    }

    koinPrice.formatAs { crypto(cryptoCurrency = "BTC", decimals = 12).shorted() }

    val cryptoCurrency: CryptoCurrency = cryptoCurrencyMock

    koinPrice.formatAs { crypto(cryptoCurrency).shorted() }

    koinPrice.formatAs(format)

    koinPrice.formatAs(format.asDefaultAmount())

    koinPrice.formatAs { format }

    koinPrice.formatAs { format.asDefaultAmount() }

    koinPrice.formatAs { format.shorted() }
}
