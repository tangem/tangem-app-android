package com.tangem.data.tokens.mock

import com.tangem.domain.tokens.model.Token
import com.tangem.domain.wallets.models.UserWalletId

internal object MockTokens {

    val token1 get() = Token(
        id = Token.ID("token1"),
        networkId = MockNetworks.network1.id,
        name = "Token 1",
        symbol = "T1",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = true,
    )
    val token2 get() = Token(
        id = Token.ID("token2"),
        networkId = MockNetworks.network1.id,
        name = "Token 2",
        symbol = "T2",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token3 get() = Token(
        id = Token.ID("token3"),
        networkId = MockNetworks.network1.id,
        name = "Token 3",
        symbol = "T3",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token4 get() = Token(
        id = Token.ID("token4"),
        networkId = MockNetworks.network2.id,
        name = "Token 4",
        symbol = "T4",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = true,
    )
    val token5 get() = Token(
        id = Token.ID("token5"),
        networkId = MockNetworks.network2.id,
        name = "Token 5",
        symbol = "T5",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token6 get() = Token(
        id = Token.ID("token6"),
        networkId = MockNetworks.network2.id,
        name = "Token 6",
        symbol = "T6",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token7 get() = Token(
        id = Token.ID("token7"),
        networkId = MockNetworks.network3.id,
        name = "Token 7",
        symbol = "T7",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = true,
    )
    val token8 get() = Token(
        id = Token.ID("token8"),
        networkId = MockNetworks.network3.id,
        name = "Token 8",
        symbol = "T8",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token9 get() = Token(
        id = Token.ID("token9"),
        networkId = MockNetworks.network3.id,
        name = "Token 9",
        symbol = "T9",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )
    val token10 get() = Token(
        id = Token.ID("token10"),
        networkId = MockNetworks.network3.id,
        name = "Token 10",
        symbol = "T10",
        isCustom = false,
        decimals = 8,
        iconUrl = null,
        isCoin = false,
    )

    val tokens get() = mapOf(
        UserWalletId(stringValue = "123") to setOf(
            token1, token2, token3, token4, token5,
            token6, token7, token8, token9, token10,
        ),
        UserWalletId(stringValue = "321") to setOf(token1, token2, token3),
        UserWalletId(stringValue = "42") to setOf(token7, token8, token9, token10),
        UserWalletId(stringValue = "24") to setOf(token4, token5, token6),
    )

    val isGrouped get() = mapOf(
        UserWalletId(stringValue = "123") to true,
        UserWalletId(stringValue = "321") to false,
        UserWalletId(stringValue = "42") to false,
        UserWalletId(stringValue = "24") to true,
    )

    val isSortedByBalance get() = mapOf(
        UserWalletId(stringValue = "123") to true,
        UserWalletId(stringValue = "321") to false,
        UserWalletId(stringValue = "42") to true,
        UserWalletId(stringValue = "24") to false,
    )
}