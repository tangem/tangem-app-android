package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.model.converter.EarnApyInfo
import com.tangem.features.foryou.impl.model.converter.EarnOpportunities
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal

/**
 * Factories for the earn-opportunities converter tests. Every argument is defaulted so a test
 * overrides only the fields it asserts on.
 */

internal fun createEarnCurrency(
    tokenId: String? = "ethereum",
    currencyId: String = "coin-ethereum",
    name: String = "Ethereum",
    networkRawId: String = "ETH",
    networkName: String = "Ethereum",
): CryptoCurrency {
    val networkId: Network.ID = mockk {
        every { rawId } returns Network.RawID(networkRawId)
    }
    val network: Network = mockk {
        every { this@mockk.name } returns networkName
        every { isTestnet } returns false
        every { rawId } returns networkRawId
        every { this@mockk.id } returns networkId
    }
    val id: CryptoCurrency.ID = mockk {
        every { rawCurrencyId } returns tokenId?.let { CryptoCurrency.RawID(it) }
        every { value } returns currencyId
    }
    return mockk<CryptoCurrency.Coin> {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
        every { this@mockk.network } returns network
        every { isCustom } returns false
        every { iconUrl } returns null
    }
}

/** A token currency whose `yieldSupplyKey()` is `"<networkRawId>_<contractAddress>"`. */
internal fun createEarnTokenCurrency(
    contractAddress: String = "0xabc",
    tokenId: String? = "usd-coin",
    currencyId: String = "token-usdc",
    name: String = "USD Coin",
    networkRawId: String = "ETH",
    networkName: String = "Ethereum",
): CryptoCurrency.Token {
    val networkId: Network.ID = mockk {
        every { rawId } returns Network.RawID(networkRawId)
    }
    val network: Network = mockk {
        every { this@mockk.name } returns networkName
        every { isTestnet } returns false
        every { rawId } returns networkRawId
        every { this@mockk.id } returns networkId
    }
    val id: CryptoCurrency.ID = mockk {
        every { rawCurrencyId } returns tokenId?.let { CryptoCurrency.RawID(it) }
        every { value } returns currencyId
    }
    return mockk {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
        every { this@mockk.network } returns network
        every { this@mockk.contractAddress } returns contractAddress
        every { isCustom } returns false
        every { iconUrl } returns null
    }
}

internal fun createEarnToken(
    apy: String = "5.5",
    networkId: String = "ethereum",
    rewardType: EarnRewardType = EarnRewardType.APY,
    type: EarnType = EarnType.STAKING,
    tokenId: String = "ethereum",
    tokenSymbol: String = "ETH",
    tokenName: String = "Ethereum",
    tokenAddress: String? = null,
    decimalCount: Int? = null,
): EarnToken = EarnToken(
    apy = apy,
    networkId = networkId,
    rewardType = rewardType,
    type = type,
    tokenId = tokenId,
    tokenSymbol = tokenSymbol,
    tokenName = tokenName,
    tokenAddress = tokenAddress,
    decimalCount = decimalCount,
)

/** A top-earn suggestion whose row id becomes `"<tokenId>-<networkRawId>"`. */
internal fun createTopEarnToken(
    tokenId: String = "ethereum",
    networkRawId: String = "ETH",
    networkName: String = "Ethereum",
    name: String = "Ethereum",
    apy: String = "5.5",
    type: EarnType = EarnType.STAKING,
    rewardType: EarnRewardType = EarnRewardType.APY,
): EarnTokenWithCurrency = EarnTokenWithCurrency(
    networkName = networkName,
    earnToken = createEarnToken(apy = apy, tokenId = tokenId, type = type, rewardType = rewardType),
    cryptoCurrency = createEarnCurrency(
        tokenId = tokenId,
        currencyId = "$tokenId-$networkRawId",
        name = name,
        networkRawId = networkRawId,
        networkName = networkName,
    ),
)

/** A fully resolved status value suitable for rendering rows (fiat amount, sources, no error). */
internal fun createRowLoadedValue(
    fiatAmount: BigDecimal = BigDecimal("100"),
    source: StatusSource = StatusSource.ACTUAL,
): CryptoCurrencyStatus.Loaded = mockk {
    every { this@mockk.fiatAmount } returns fiatAmount
    every { isError } returns false
    every { sources } returns CryptoCurrencyStatus.Sources(
        networkSource = source,
        quoteSource = source,
        stakingBalanceSource = source,
    )
}

/** A status value carrying the earn-related fields read by `ForYouEarnOpportunitiesConverter`. */
internal fun createEarnStatusValue(
    fiatAmount: BigDecimal = BigDecimal("100"),
    yieldSupplyActive: Boolean? = null,
    isStakingActive: Boolean = false,
    stakingBalance: StakingBalance? = if (isStakingActive) mockk<StakingBalance.Data.P2PEthPool>() else null,
): CryptoCurrencyStatus.Loaded = mockk {
    every { this@mockk.fiatAmount } returns fiatAmount
    every { yieldSupplyStatus } returns yieldSupplyActive?.let { active ->
        mockk<YieldSupplyStatus> { every { isActive } returns active }
    }
    every { this@mockk.stakingBalance } returns stakingBalance
    every { isError } returns false
    every { sources } returns CryptoCurrencyStatus.Sources()
}

internal fun createStatus(
    currency: CryptoCurrency,
    value: CryptoCurrencyStatus.Value = CryptoCurrencyStatus.Loading,
): CryptoCurrencyStatus = CryptoCurrencyStatus(currency = currency, value = value)

internal fun createEarnOpportunities(
    account: Account.CryptoPortfolio = MockAccounts.createAccount(derivationIndex = 1),
    earnCurrencies: Map<CryptoCurrencyStatus, EarnApyInfo> = mapOf(
        createStatus(createEarnCurrency()) to createEarnApyInfo(),
    ),
    accountPotentialReward: BigDecimal = BigDecimal.ZERO,
): EarnOpportunities = EarnOpportunities(
    account = account,
    earnCurrencies = earnCurrencies,
    accountPotentialReward = accountPotentialReward,
)

internal fun createEarnApyInfo(
    isActive: Boolean = true,
    apy: BigDecimal? = BigDecimal("0.05"),
    potentialRewards: BigDecimal? = null,
    type: ForYouEarnOpportunitiesType = ForYouEarnOpportunitiesType.YieldSupply(apy = "5.5"),
): EarnApyInfo = EarnApyInfo(isActive = isActive, apy = apy, potentialRewards = potentialRewards, type = type)

internal fun createPortfolioStatus(
    currencies: List<CryptoCurrencyStatus>,
    account: Account.CryptoPortfolio = MockAccounts.createAccount(derivationIndex = 1),
): AccountStatus.CryptoPortfolio = mockk {
    every { flattenCurrencies() } returns currencies
    every { this@mockk.account } returns account
}

internal fun createAccountStatusList(vararg statuses: AccountStatus): AccountStatusList = mockk {
    every { accountStatuses } returns statuses.toList()
    every { userWalletId } returns MockAccounts.userWalletId
}