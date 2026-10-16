package com.tangem.test.mock.yield

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Yield Mode fixtures, every argument defaulted so a test states only what it is about.
 *
 * The defaults describe one supplied token on Ethereum with a single owner-bound reward, which is the shape most
 * Yield Mode tests start from.
 */
object MockYieldSupply {

    const val NETWORK_ID = "ethereum"
    const val CHAIN_ID = 1
    const val TOKEN_ADDRESS = "0xTokenAbC"
    const val USER_ADDRESS = "0xUser"
    const val MODULE_ADDRESS = "0xModule"
    const val REWARD_TOKEN = "0xWETH"

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
        rewardsApy: BigDecimal = BigDecimal("1.5"),
        totalApyEst: BigDecimal = BigDecimal("5.5"),
    ) = YieldIncentiveCampaign(
        hasActiveCampaign = hasActiveCampaign,
        rewardsApy = rewardsApy,
        totalApyEst = totalApyEst,
    )

    @Suppress("LongParameterList")
    fun reward(
        rewardToken: String = REWARD_TOKEN,
        symbol: String = "WETH",
        tokenDecimals: Int = 18,
        claimableAmount: BigInteger = BigInteger("1000000000000000000"),
        pendingAmount: BigInteger = BigInteger.ZERO,
        usdValue: BigDecimal = BigDecimal("10.00"),
        estimatedNetValueUsd: BigDecimal = BigDecimal("8.50"),
        estimatedServiceFeeUsd: BigDecimal = BigDecimal("1.50"),
        serviceFeeRate: BigDecimal = BigDecimal("0.15"),
        finalDestination: YieldRewardDestination = YieldRewardDestination.Owner,
    ) = YieldReward(
        rewardToken = rewardToken,
        symbol = symbol,
        tokenDecimals = tokenDecimals,
        claimableAmount = claimableAmount,
        pendingAmount = pendingAmount,
        usdValue = usdValue,
        estimatedNetValueUsd = estimatedNetValueUsd,
        estimatedServiceFeeUsd = estimatedServiceFeeUsd,
        serviceFeeRate = serviceFeeRate,
        finalDestination = finalDestination,
    )

    @Suppress("LongParameterList")
    fun calldata(
        chainId: Int = CHAIN_ID,
        userAddress: String = USER_ADDRESS,
        moduleAddress: String? = MODULE_ADDRESS,
        hasClaimable: Boolean = true,
        to: String? = MODULE_ADDRESS,
        data: String? = "0xdeadbeef",
        rewardTokens: List<String> = listOf(REWARD_TOKEN),
        cumulativeAmounts: List<String> = listOf("1000000000000000000"),
        totalEstimatedServiceFeeUsd: BigDecimal = BigDecimal("1.50"),
        isClaimRecommended: Boolean = true,
    ) = YieldClaimCalldata(
        chainId = chainId,
        userAddress = userAddress,
        moduleAddress = moduleAddress,
        hasClaimable = hasClaimable,
        to = to,
        data = data,
        rewardTokens = rewardTokens,
        cumulativeAmounts = cumulativeAmounts,
        totalEstimatedServiceFeeUsd = totalEstimatedServiceFeeUsd,
        isClaimRecommended = isClaimRecommended,
    )

    fun claimableSummary(
        moduleAddress: String = MODULE_ADDRESS,
        claimable: List<YieldReward> = listOf(reward()),
        pending: List<YieldReward> = emptyList(),
        unsupported: List<YieldReward> = emptyList(),
        supportedRewardTokens: Set<String> = setOf(REWARD_TOKEN.lowercase()),
        totalNetUsd: BigDecimal = BigDecimal("8.50"),
    ) = YieldRewardsSummary.Claimable(
        moduleAddress = moduleAddress,
        claimable = claimable,
        pending = pending,
        unsupported = unsupported,
        supportedRewardTokens = supportedRewardTokens,
        totalNetUsd = totalNetUsd,
    )

    fun deferredSummary(
        moduleAddress: String = MODULE_ADDRESS,
        claimable: List<YieldReward> = listOf(reward()),
        pending: List<YieldReward> = emptyList(),
        unsupported: List<YieldReward> = emptyList(),
        supportedRewardTokens: Set<String> = setOf(REWARD_TOKEN.lowercase()),
        totalNetUsd: BigDecimal = BigDecimal("8.50"),
    ) = YieldRewardsSummary.Deferred(
        moduleAddress = moduleAddress,
        claimable = claimable,
        pending = pending,
        unsupported = unsupported,
        supportedRewardTokens = supportedRewardTokens,
        totalNetUsd = totalNetUsd,
    )

    fun network(
        id: String = NETWORK_ID,
        name: String = "Ethereum",
        currencySymbol: String = "ETH",
        derivationPath: Network.DerivationPath = Network.DerivationPath.None,
    ) = Network(
        id = Network.ID(value = id, derivationPath = derivationPath),
        name = name,
        currencySymbol = currencySymbol,
        derivationPath = derivationPath,
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

    fun loadedStatus(
        currency: CryptoCurrency = token(),
        amount: BigDecimal = BigDecimal.ONE,
        fiatAmount: BigDecimal = BigDecimal.ONE,
        yieldSupplyStatus: YieldSupplyStatus? = null,
        networkSource: StatusSource = StatusSource.ACTUAL,
    ) = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = fiatAmount,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = yieldSupplyStatus,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = USER_ADDRESS,
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(networkSource = networkSource),
        ),
    )

    fun yieldSupplyStatus(isActive: Boolean = true) = YieldSupplyStatus(
        isActive = isActive,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = null,
    )

    fun earnToken(apy: String = "0.092", networkId: String = NETWORK_ID, tokenSymbol: String = "USDT") = EarnToken(
        apy = apy,
        networkId = networkId,
        rewardType = EarnRewardType.APY,
        type = EarnType.YIELD,
        tokenId = tokenSymbol.lowercase(),
        tokenSymbol = tokenSymbol,
        tokenName = tokenSymbol,
        tokenAddress = TOKEN_ADDRESS,
        decimalCount = 6,
    )
}