package com.tangem.test.mock.yield

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import java.math.BigDecimal

/**
 * Yield Mode fixtures, every argument defaulted so a test states only what it is about.
 *
 * The defaults describe one supplied token on Ethereum, which is the shape most Yield Mode tests start from.
 */
object MockYieldSupply {

    const val NETWORK_ID = "ethereum"
    const val CHAIN_ID = 1
    const val TOKEN_ADDRESS = "0xTokenAbC"
    const val USER_ADDRESS = "0xUser"

    fun market(
        tokenAddress: String = TOKEN_ADDRESS,
        chainId: Int = CHAIN_ID,
        apy: BigDecimal = BigDecimal("3.09"),
        isActive: Boolean = true,
        maxFeeNative: BigDecimal = BigDecimal.ZERO,
        maxFeeUSD: BigDecimal = BigDecimal.ZERO,
        backendId: String? = NETWORK_ID,
        campaign: YieldIncentiveCampaign? = null,
    ) = YieldMarketToken(
        tokenAddress = tokenAddress,
        chainId = chainId,
        apy = apy,
        isActive = isActive,
        maxFeeNative = maxFeeNative,
        maxFeeUSD = maxFeeUSD,
        backendId = backendId,
        campaign = campaign,
    )

    fun campaign(
        hasActiveCampaign: Boolean = true,
        rewardsApr: BigDecimal = BigDecimal("1.5"),
        totalApyEst: BigDecimal = BigDecimal("5.5"),
    ) = YieldIncentiveCampaign(
        hasActiveCampaign = hasActiveCampaign,
        rewardsApr = rewardsApr,
        totalApyEst = totalApyEst,
    )

    fun network(id: String = NETWORK_ID, name: String = "Ethereum", currencySymbol: String = "ETH") = Network(
        id = Network.ID(value = id, derivationPath = Network.DerivationPath.None),
        name = name,
        currencySymbol = currencySymbol,
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )

    fun token(
        contractAddress: String = TOKEN_ADDRESS,
        network: Network = network(),
        name: String = "TEST_TOKEN",
        symbol: String = "TTK",
        decimals: Int = 6,
    ) = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
            suffix = CryptoCurrency.ID.Suffix.RawID(contractAddress),
        ),
        network = network,
        name = name,
        symbol = symbol,
        decimals = decimals,
        iconUrl = null,
        isCustom = false,
        contractAddress = contractAddress,
    )

    fun coin(network: Network = network()) = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
            suffix = CryptoCurrency.ID.Suffix.RawID(network.rawId),
        ),
        network = network,
        name = "TEST_COIN",
        symbol = network.currencySymbol,
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )
}