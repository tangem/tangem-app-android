package com.tangem.feature.swap.mcp

import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.SwapAmountType

@Suppress("LongParameterList")
internal fun createExchangeIntent(
    id: String = "my_intent_id123",
    isHandled: Boolean = false,
    clientId: String? = "claude-code-client",
    sessionId: String? = "session-id",
    amountType: SwapAmountType = SwapAmountType.From,
    amount: String = "42000000",
    from: ExchangeIntent.Asset = ExchangeIntent.Asset(
        network = "ethereum",
        contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
        decimals = 6,
        address = null,
    ),
    to: ExchangeIntent.Asset = ExchangeIntent.Asset(
        network = "polygon-pos",
        contractAddress = "0",
        decimals = 18,
        address = null,
    ),
    providerId: String = "changelly",
    rateType: ExpressRateType = ExpressRateType.Float,
) = ExchangeIntent(
    id = id,
    walletId = "wallet-id",
    clientId = clientId,
    sessionId = sessionId,
    isHandled = isHandled,
    amountType = amountType,
    amount = amount,
    from = from,
    to = to,
    providerId = providerId,
    rateType = rateType,
    toExtraId = null,
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

internal fun createCoin(network: Network = createNetwork(), symbol: String = "ETH"): CryptoCurrency.Coin =
    CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.create(rawId = network.rawId, derivationPath = network.derivationPath),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = network.rawId),
        ),
        network = network,
        name = network.name,
        symbol = symbol,
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )

internal fun createToken(
    network: Network = createNetwork(),
    contractAddress: String = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
    name: String = "Tether",
    symbol: String = "USDT",
    iconUrl: String? = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
): CryptoCurrency.Token = CryptoCurrency.Token(
    id = CryptoCurrency.ID(
        prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
        body = CryptoCurrency.ID.Body.create(rawId = network.rawId, derivationPath = network.derivationPath),
        suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "tether", contractAddress = contractAddress),
    ),
    network = network,
    name = name,
    symbol = symbol,
    decimals = 6,
    iconUrl = iconUrl,
    isCustom = false,
    contractAddress = contractAddress,
)