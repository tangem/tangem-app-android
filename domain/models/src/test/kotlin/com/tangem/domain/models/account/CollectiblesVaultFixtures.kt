package com.tangem.domain.models.account

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import java.math.BigDecimal

internal fun vaultBalance(amount: String, rate: String?) = CollectiblesVaultBalance(
    usdcToken = VAULT_TOKEN,
    usdcAmount = BigDecimal(amount),
    usdcFiatRate = rate?.let(::BigDecimal),
)

private const val NETWORK_ID = "solana/test"

private val VAULT_NETWORK = Network(
    id = Network.ID(value = NETWORK_ID, derivationPath = Network.DerivationPath.None),
    name = "Solana Testnet",
    currencySymbol = "SOL",
    derivationPath = Network.DerivationPath.None,
    isTestnet = true,
    standardType = Network.StandardType.Unspecified("Solana"),
    hasFiatFeeRate = true,
    canHandleTokens = true,
    transactionExtrasType = Network.TransactionExtrasType.NONE,
    nameResolvingType = Network.NameResolvingType.NONE,
)

private val VAULT_TOKEN = CryptoCurrency.Token(
    id = CryptoCurrency.ID(
        prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
        body = CryptoCurrency.ID.Body.NetworkId(rawId = NETWORK_ID),
        suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "usd-coin"),
    ),
    network = VAULT_NETWORK,
    name = "USD Coin",
    symbol = "USDC",
    decimals = 6,
    iconUrl = null,
    isCustom = false,
    contractAddress = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU",
)