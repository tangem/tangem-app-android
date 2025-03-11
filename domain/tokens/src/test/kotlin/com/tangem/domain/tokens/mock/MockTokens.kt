package com.tangem.domain.tokens.mock

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrency.ID

internal object MockTokens {

    val token1
        get() = CryptoCurrency.Coin(
            id = ID(
                ID.Prefix.COIN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network1.id.value),
                ID.Suffix.RawID("token1"),
            ),
            network = MockNetworks.network1,
            name = "Token 1",
            symbol = "T1",
            decimals = 8,
            iconUrl = null,
            isCustom = false,
        )
    val token2
        get() = CryptoCurrency.Token(
            id = ID(
                ID.Prefix.TOKEN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network1.id.value),
                ID.Suffix.RawID("token2"),
            ),
            network = MockNetworks.network1,
            name = "Token 2",
            symbol = "T2",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
        )
    val token3
        get() = CryptoCurrency.Token(
            id = ID(
                ID.Prefix.TOKEN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network1.id.value),
                ID.Suffix.RawID("token3"),
            ),
            network = MockNetworks.network1,
            name = "Token 3",
            symbol = "T3",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
        )
    val token4
        get() = CryptoCurrency.Coin(
            id = ID(
                ID.Prefix.COIN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network2.id.value),
                ID.Suffix.RawID("token4"),
            ),
            network = MockNetworks.network2,
            name = "Token 4",
            symbol = "T4",
            decimals = 8,
            iconUrl = null,
            isCustom = false,
        )
    val token5
        get() = CryptoCurrency.Token(
            id = ID(
                ID.Prefix.TOKEN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network2.id.value),
                ID.Suffix.RawID("token5"),
            ),
            network = MockNetworks.network2,
            name = "Token 5",
            symbol = "T5",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
        )
    val token6
        get() = CryptoCurrency.Token(
            id = ID(
                ID.Prefix.TOKEN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network2.id.value),
                ID.Suffix.RawID("token6"),
            ),
            network = MockNetworks.network2,
            name = "Token 6",
            symbol = "T6",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
        )
    val token7
        get() = CryptoCurrency.Coin(
            id = ID(
                ID.Prefix.COIN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network3.id.value),
                ID.Suffix.RawID("token7"),
            ),
            network = MockNetworks.network3,
            name = "Token 7",
            symbol = "T7",
            decimals = 8,
            iconUrl = null,
            isCustom = false,
        )
    val token8
        get() = CryptoCurrency.Token(
            id = ID(
                ID.Prefix.TOKEN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network3.id.value),
                ID.Suffix.RawID("token8"),
            ),
            network = MockNetworks.network3,
            name = "Token 8",
            symbol = "T8",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
        )
    val token9
        get() = CryptoCurrency.Token(
            id = ID(
                ID.Prefix.TOKEN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network3.id.value),
                ID.Suffix.RawID("token9"),
            ),
            network = MockNetworks.network3,
            name = "Token 9",
            symbol = "T9",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
        )
    val token10
        get() = CryptoCurrency.Token(
            id = ID(
                ID.Prefix.TOKEN_PREFIX,
                ID.Body.NetworkId(MockNetworks.network3.id.value),
                ID.Suffix.RawID("token10"),
            ),
            network = MockNetworks.network3,
            name = "Token 10",
            symbol = "T10",
            isCustom = false,
            decimals = 8,
            iconUrl = null,
            contractAddress = "address",
        )

    val tokens = listOf(token1, token2, token3, token4, token5, token6, token7, token8, token9, token10)
}