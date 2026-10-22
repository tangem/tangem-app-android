package com.tangem.features.send.mcp

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.models.SendIntent
import java.math.BigDecimal

internal const val SENDER_ADDRESS = "0x7a2f3c9e1b4d5a6f8e0c2b3d4e5f6a7b8c9d0e1f"

@Suppress("LongParameterList")
internal fun createSendIntent(
    id: String = "my_intent_id123",
    clientId: String = "claude-code-client",
    sessionId: String = "session-id",
    isHandled: Boolean = false,
    networkId: String = "ethereum",
    tokenId: String = "tether",
    fromAddress: String = SENDER_ADDRESS,
    toAddress: String = "0x1d0c4e8f2a3b5c6d7e8f9a0b1c2d3e4f5a6b7c8d",
    amount: BigDecimal = BigDecimal("42"),
    memo: String? = null,
    comment: String? = null,
) = SendIntent(
    id = id,
    clientId = clientId,
    sessionId = sessionId,
    isHandled = isHandled,
    networkId = networkId,
    tokenId = tokenId,
    fromAddress = fromAddress,
    toAddress = toAddress,
    amount = amount,
    memo = memo,
    comment = comment,
)

internal fun createNetwork(rawId: String = "ethereum", name: String = "Ethereum"): Network {
    val derivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0")

    return Network(
        id = Network.ID(rawId = Network.RawID(rawId), derivationPath = derivationPath),
        name = name,
        currencySymbol = "ETH",
        derivationPath = derivationPath,
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )
}

internal fun createCoin(
    network: Network = createNetwork(),
    rawId: String = network.rawId,
    symbol: String = "ETH",
): CryptoCurrency.Coin = CryptoCurrency.Coin(
    id = CryptoCurrency.ID(
        prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
        body = CryptoCurrency.ID.Body.create(rawId = network.rawId, derivationPath = network.derivationPath),
        suffix = CryptoCurrency.ID.Suffix.RawID(rawId = rawId),
    ),
    network = network,
    name = network.name,
    symbol = symbol,
    decimals = 18,
    iconUrl = null,
    isCustom = false,
)

/** A [rawId] of `null` makes a custom token known only by its contract address. */
@Suppress("LongParameterList")
internal fun createToken(
    network: Network = createNetwork(),
    rawId: String? = "tether",
    contractAddress: String = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
    name: String = "Tether",
    symbol: String = "USDT",
    iconUrl: String? = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
): CryptoCurrency.Token = CryptoCurrency.Token(
    id = CryptoCurrency.ID(
        prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
        body = CryptoCurrency.ID.Body.create(rawId = network.rawId, derivationPath = network.derivationPath),
        suffix = if (rawId != null) {
            CryptoCurrency.ID.Suffix.RawID(rawId = rawId, contractAddress = contractAddress)
        } else {
            CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = contractAddress)
        },
    ),
    network = network,
    name = name,
    symbol = symbol,
    decimals = 6,
    iconUrl = iconUrl,
    isCustom = rawId == null,
    contractAddress = contractAddress,
)