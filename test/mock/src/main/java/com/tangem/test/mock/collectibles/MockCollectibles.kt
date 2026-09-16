package com.tangem.test.mock.collectibles

import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import java.math.BigDecimal

/** Fixtures for the gacha vault's balance, whose token is described by the app rather than read from the chain. */
object MockCollectibles {

    /** Circle's devnet USDC mint — any base58 string would do, this one keeps the fixtures recognisable. */
    const val USDC_MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"
    const val USDC_DECIMALS = 6

    private const val NETWORK_ID = "solana/test"

    val usdcNetwork = Network(
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

    val usdc: CryptoCurrency.Token = usdc()

    fun usdc(mint: String = USDC_MINT, decimals: Int = USDC_DECIMALS) = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = NETWORK_ID),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "usd-coin"),
        ),
        network = usdcNetwork,
        name = "USD Coin",
        symbol = "USDC",
        decimals = decimals,
        iconUrl = null,
        isCustom = false,
        contractAddress = mint,
    )

    fun vaultBalance(
        amount: BigDecimal,
        fiatRate: BigDecimal? = null,
        currency: CryptoCurrency.Token = usdc,
    ): CollectiblesVaultBalance = CollectiblesVaultBalance(
        usdcToken = currency,
        usdcAmount = amount,
        usdcFiatRate = fiatRate,
    )
}