package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import com.tangem.domain.tokens.model.Token

internal object MockTokens {

    val token1 = Token(
        id = Token.ID("token1"),
        networkId = MockNetworks.network1.id,
        name = "Token 1",
        symbol = "T1",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = true,

    )
    val token2 = Token(
        id = Token.ID("token2"),
        networkId = MockNetworks.network1.id,
        name = "Token 2",
        symbol = "T2",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token3 = Token(
        id = Token.ID("token3"),
        networkId = MockNetworks.network1.id,
        name = "Token 3",
        symbol = "T3",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token4 = Token(
        id = Token.ID("token4"),
        networkId = MockNetworks.network2.id,
        name = "Token 4",
        symbol = "T4",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = true,

    )
    val token5 = Token(
        id = Token.ID("token5"),
        networkId = MockNetworks.network2.id,
        name = "Token 5",
        symbol = "T5",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token6 = Token(
        id = Token.ID("token6"),
        networkId = MockNetworks.network2.id,
        name = "Token 6",
        symbol = "T6",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token7 = Token(
        id = Token.ID("token7"),
        networkId = MockNetworks.network3.id,
        name = "Token 7",
        symbol = "T7",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = true,
    )
    val token8 = Token(
        id = Token.ID("token8"),
        networkId = MockNetworks.network3.id,
        name = "Token 8",
        symbol = "T8",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token9 = Token(
        id = Token.ID("token9"),
        networkId = MockNetworks.network3.id,
        name = "Token 9",
        symbol = "T9",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token10 = Token(
        id = Token.ID("token10"),
        networkId = MockNetworks.network3.id,
        name = "Token 10",
        symbol = "T10",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )

    val tokens = nonEmptySetOf(token1, token2, token3, token4, token5, token6, token7, token8, token9, token10)
}