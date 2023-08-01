package com.tangem.domain.tokens.mock

import com.tangem.domain.tokens.model.CryptoCurrency

internal object MockTokens {

    val token1
        get() = CryptoCurrency.Coin(
            id = CryptoCurrency.ID("token1"),
            networkId = MockNetworks.network1.id,
            name = "Token 1",
            symbol = "T1",
            decimals = 8,
            iconUrl = null,
            derivationPath = null,
        )
    val token2
        get() = CryptoCurrency.Token(
            id = CryptoCurrency.ID("token2"),
            networkId = MockNetworks.network1.id,
            name = "Token 2",
            symbol = "T2",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
            derivationPath = null,
        )
    val token3
        get() = CryptoCurrency.Token(
            id = CryptoCurrency.ID("token3"),
            networkId = MockNetworks.network1.id,
            name = "Token 3",
            symbol = "T3",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
            derivationPath = null,
        )
    val token4
        get() = CryptoCurrency.Coin(
            id = CryptoCurrency.ID("token4"),
            networkId = MockNetworks.network2.id,
            name = "Token 4",
            symbol = "T4",
            decimals = 8,
            iconUrl = null,
            derivationPath = null,
        )
    val token5
        get() = CryptoCurrency.Token(
            id = CryptoCurrency.ID("token5"),
            networkId = MockNetworks.network2.id,
            name = "Token 5",
            symbol = "T5",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
            derivationPath = null,
        )
    val token6
        get() = CryptoCurrency.Token(
            id = CryptoCurrency.ID("token6"),
            networkId = MockNetworks.network2.id,
            name = "Token 6",
            symbol = "T6",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
            derivationPath = null,
        )
    val token7
        get() = CryptoCurrency.Coin(
            id = CryptoCurrency.ID("token7"),
            networkId = MockNetworks.network3.id,
            name = "Token 7",
            symbol = "T7",
            decimals = 8,
            iconUrl = null,
            derivationPath = null,
        )
    val token8
        get() = CryptoCurrency.Token(
            id = CryptoCurrency.ID("token8"),
            networkId = MockNetworks.network3.id,
            name = "Token 8",
            symbol = "T8",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
            derivationPath = null,
        )
    val token9
        get() = CryptoCurrency.Token(
            id = CryptoCurrency.ID("token9"),
            networkId = MockNetworks.network3.id,
            name = "Token 9",
            symbol = "T9",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
            derivationPath = null,
        )
    val token10
        get() = CryptoCurrency.Token(
            id = CryptoCurrency.ID("token10"),
            networkId = MockNetworks.network3.id,
            name = "Token 10",
            symbol = "T10",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
            derivationPath = null,
        )

    val tokens = setOf(token1, token2, token3, token4, token5, token6, token7, token8, token9, token10)
}