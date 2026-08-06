package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.earn.EarnType
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouWalletHeaderUM
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.features.foryou.impl.model.converter.EarnApyInfo
import com.tangem.features.foryou.impl.model.converter.EarnOpportunities
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal

/** All token items across every wallet group, for tests asserting on the flat item list. */
internal val EarnOpportunitiesUM.items: List<ForYouTokenListItemUM>
    get() = tokenList.flatMap { it.items }

internal fun createWalletHeader(
    userWalletId: UserWalletId,
    name: String = "Wallet ${userWalletId.stringValue}",
): ForYouWalletHeaderUM = ForYouWalletHeaderUM(
    id = userWalletId.stringValue,
    name = stringReference(name),
    deviceIcon = DeviceIconUM.Stub(cardsCount = 1),
)

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

/**
 * A status value carrying the earn-related fields read by `ForYouEarnOpportunitiesConverter`.
 *
 * [useContributions] switches between the two architectures the way `CryptoCurrencyStatusFactory` does: with the
 * toggle on the balance arrives in `contributions` and the typed field is **null**. Populating both would let a
 * dead contributions path pass off the legacy fallback.
 */
internal fun createEarnStatusValue(
    fiatAmount: BigDecimal = BigDecimal("100"),
    yieldSupplyActive: Boolean? = null,
    isStakingActive: Boolean = false,
    stakingBalance: StakingBalance? = if (isStakingActive) createP2PEthPoolBalance() else null,
    useContributions: Boolean = false,
): CryptoCurrencyStatus.Loaded = mockk {
    every { this@mockk.fiatAmount } returns fiatAmount
    every { yieldSupplyStatus } returns yieldSupplyActive?.let { active ->
        mockk<YieldSupplyStatus> { every { isActive } returns active }
    }
    every { this@mockk.stakingBalance } returns stakingBalance.takeIf { !useContributions }
    // The staking accessor reads contributions before falling back to the typed field, so a strict mock has to
    // answer it either way.
    every { contributions } returns if (useContributions) {
        listOfNotNull(stakingBalance as? BalanceContribution)
    } else {
        emptyList()
    }
    every { isError } returns false
    every { sources } returns CryptoCurrencyStatus.Sources()
}

/**
 * A **real** balance, not a mock: `totalDeltaCryptoAmount()` is a member function, so MockK would intercept it and
 * a relaxed mock would return a mocked `BigDecimal` — an assertion could pass while testing nothing.
 */
internal fun createP2PEthPoolBalance(): StakingBalance.Data.P2PEthPool = StakingBalance.Data.P2PEthPool(
    stakingId = StakingID(integrationId = "integration", address = "0x1"),
    source = StatusSource.ACTUAL,
    accounts = emptyList(),
)

internal fun createStatus(
    currency: CryptoCurrency,
    value: CryptoCurrencyStatus.Value = CryptoCurrencyStatus.Loading,
): CryptoCurrencyStatus = CryptoCurrencyStatus(currency = currency, value = value)

internal fun createEarnOpportunities(
    userWalletId: UserWalletId = MockAccounts.userWalletId,
    account: Account.CryptoPortfolio = MockAccounts.createAccount(derivationIndex = 1),
    earnCurrencies: Map<CryptoCurrencyStatus, EarnApyInfo> = mapOf(
        createStatus(createEarnCurrency()) to createEarnApyInfo(),
    ),
    accountPotentialReward: BigDecimal = BigDecimal.ZERO,
): EarnOpportunities = EarnOpportunities(
    userWalletId = userWalletId,
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
    every { accountId } returns account.accountId
}

internal fun createAccountStatusList(vararg statuses: AccountStatus): AccountStatusList = mockk {
    every { accountStatuses } returns statuses.toList()
    every { userWalletId } returns MockAccounts.userWalletId
}

/** Builds the selected-portfolio aggregate consumed by the earn/review converters. */
internal fun createSelectedPortfolio(
    vararg accounts: AccountStatus.CryptoPortfolio,
    totalAccountsCount: Int = accounts.size,
    totalFiatBalance: TotalFiatBalance =
        TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
): ForYouSelectedPortfolio {
    val accountCryptoCurrencyStatuses = accounts.flatMap { accountStatus ->
        accountStatus.flattenCurrencies().map { currencyStatus ->
            mockk<AccountCryptoCurrencyStatus> {
                every { account } returns accountStatus.account
                every { status } returns currencyStatus
            }
        }
    }
    return ForYouSelectedPortfolio(
        accountCryptoCurrencyStatuses = accountCryptoCurrencyStatuses,
        totalAccountsCount = totalAccountsCount,
        totalFiatBalance = totalFiatBalance,
    )
}