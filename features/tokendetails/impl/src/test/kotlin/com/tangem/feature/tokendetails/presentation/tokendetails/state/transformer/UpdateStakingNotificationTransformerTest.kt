package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.StakingOption
import com.tangem.utils.StringsSigns.THREE_STARS
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UpdateStakingNotificationTransformerTest {

    private val clickIntents: TokenDetailsClickIntents = mockk(relaxed = true)

    @Test
    fun `GIVEN Unavailable WHEN transform THEN earnBlockState is null`() {
        val transformer = createTransformer(
            availability = StakingAvailability.Unavailable,
            entryInfo = null,
        )

        val result = transformer.transform(initialState())

        assertThat(result.earnBlockState).isNull()
    }

    @Test
    fun `GIVEN TemporaryUnavailable WHEN transform THEN TemporaryUnavailable`() {
        val transformer = createTransformer(
            availability = StakingAvailability.TemporaryUnavailable,
            entryInfo = null,
        )

        val result = transformer.transform(initialState())

        assertThat(result.earnBlockState).isInstanceOf(EarnBlockUM.Content::class.java)
        val content = result.earnBlockState as EarnBlockUM.Content
        assertThat(content.iconUM).isInstanceOf(EarnBlockUM.IconUM.Plain::class.java)
        assertThat(content.trailingUM).isNull()
    }

    @Test
    fun `GIVEN Available without entryInfo AND no staked WHEN transform THEN null`() {
        val transformer = createTransformer(
            availability = availableOption(BigDecimal("4.2")),
            entryInfo = null,
        )

        val result = transformer.transform(initialState())

        assertThat(result.earnBlockState).isNull()
    }

    @Test
    fun `GIVEN Available with entryInfo AND no staked WHEN transform THEN Available`() {
        val transformer = createTransformer(
            availability = availableOption(BigDecimal("4.2")),
            entryInfo = StakingEntryInfo(tokenSymbol = "SOL"),
        )

        val result = transformer.transform(initialState())

        assertThat(result.earnBlockState).isInstanceOf(EarnBlockUM.Content::class.java)
        val content = result.earnBlockState as EarnBlockUM.Content
        assertThat(content.trailingUM).isInstanceOf(EarnBlockUM.TrailingUM.Button::class.java)
    }

    @Test
    fun `GIVEN Full AND no active stake WHEN transform THEN earnBlockState is null`() {
        val transformer = createTransformer(
            availability = fullOption(BigDecimal("4.2")),
            entryInfo = null,
        )

        val result = transformer.transform(initialState())

        assertThat(result.earnBlockState).isNull()
    }

    @Test
    fun `GIVEN Full AND active stake WHEN transform THEN active balance block`() {
        val transformer = createTransformer(
            availability = fullOption(BigDecimal("4.2")),
            entryInfo = null,
            status = buildStatusWithStake(stakedAmount = BigDecimal("5")),
        )

        val result = transformer.transform(initialState())

        assertThat(result.earnBlockState).isInstanceOf(EarnBlockUM.Content::class.java)
        val content = result.earnBlockState as EarnBlockUM.Content
        assertThat(content.trailingUM).isInstanceOf(EarnBlockUM.TrailingUM.Balance::class.java)
    }

    @Test
    fun `GIVEN active staked balance AND balance hidden WHEN transform THEN trailing balance hidden`() {
        // Arrange
        val status = buildStatus(
            networkRawId = "ethereum",
            symbol = "ETH",
            isCoin = false,
            stakingBalance = stakeKitBalance(staked = BigDecimal("100"), rewards = BigDecimal("5")),
        )
        val transformer = createTransformer(
            availability = availableOption(BigDecimal("4.2")),
            entryInfo = StakingEntryInfo(tokenSymbol = "ETH"),
            status = status,
            isBalanceHidden = true,
        )

        // Act
        val result = transformer.transform(initialState())

        // Assert
        val content = result.earnBlockState as EarnBlockUM.Content
        val trailing = content.trailingUM as EarnBlockUM.TrailingUM.Balance
        assertThat(trailing.isBalanceHidden).isTrue()
    }

    @Test
    fun `GIVEN rewards to claim AND balance hidden WHEN transform THEN reward amount masked with stars`() {
        // Arrange
        val status = buildStatus(
            networkRawId = "ethereum",
            symbol = "ETH",
            isCoin = false,
            stakingBalance = stakeKitBalance(staked = BigDecimal("100"), rewards = BigDecimal("5")),
        )
        val transformer = createTransformer(
            availability = availableOption(BigDecimal("4.2")),
            entryInfo = StakingEntryInfo(tokenSymbol = "ETH"),
            status = status,
            isBalanceHidden = true,
        )

        // Act
        val result = transformer.transform(initialState())

        // Assert
        val content = result.earnBlockState as EarnBlockUM.Content
        val subtitle = content.subtitleUM as EarnBlockUM.SubtitleUM.Text
        assertThat(rewardFormatArg(subtitle.text)).isEqualTo(THREE_STARS)
    }

    @Test
    fun `GIVEN rewards to claim AND balance visible WHEN transform THEN reward amount not masked`() {
        // Arrange
        val status = buildStatus(
            networkRawId = "ethereum",
            symbol = "ETH",
            isCoin = false,
            stakingBalance = stakeKitBalance(staked = BigDecimal("100"), rewards = BigDecimal("5")),
        )
        val transformer = createTransformer(
            availability = availableOption(BigDecimal("4.2")),
            entryInfo = StakingEntryInfo(tokenSymbol = "ETH"),
            status = status,
            isBalanceHidden = false,
        )

        // Act
        val result = transformer.transform(initialState())

        // Assert
        val content = result.earnBlockState as EarnBlockUM.Content
        val subtitle = content.subtitleUM as EarnBlockUM.SubtitleUM.Text
        assertThat(rewardFormatArg(subtitle.text)).isNotEqualTo(THREE_STARS)
    }

    private fun rewardFormatArg(text: TextReference): Any? =
        (text as TextReference.Res).formatArgs.data.firstOrNull()

    private fun createTransformer(
        availability: StakingAvailability,
        entryInfo: StakingEntryInfo?,
        status: CryptoCurrencyStatus = buildStatus(),
        isBalanceHidden: Boolean = false,
    ) = UpdateStakingNotificationTransformer(
        cryptoCurrencyStatus = status,
        stakingAvailability = availability,
        stakingEntryInfo = entryInfo,
        appCurrency = AppCurrency.Default,
        isBalanceHidden = isBalanceHidden,
        clickIntents = clickIntents,
    )

    private fun buildStatus(
        networkRawId: String = "solana",
        symbol: String = "SOL",
        isCoin: Boolean = true,
        stakingBalance: StakingBalance = mockk(relaxed = true),
    ): CryptoCurrencyStatus {
        val network = mockk<Network>(relaxed = true) {
            every { rawId } returns networkRawId
            every { isTestnet } returns false
        }
        val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { this@mockk.symbol } returns symbol
            every { decimals } returns 9
            every { this@mockk.network } returns network
            every { id.isCoin } returns isCoin
        }
        val value = mockk<CryptoCurrencyStatus.Value>(relaxed = true) {
            every { this@mockk.stakingBalance } returns stakingBalance
            every { fiatRate } returns BigDecimal.ONE
            every { yieldSupplyStatus } returns null
        }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    /** Builds an active StakeKit balance with the given [staked] and [rewards] amounts. */
    private fun stakeKitBalance(staked: BigDecimal, rewards: BigDecimal): StakingBalance.Data.StakeKit {
        val stakingId = StakingID(integrationId = "ethereum-eth-native-staking", address = "0xabc")
        return StakingBalance.Data.StakeKit(
            stakingId = stakingId,
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                integrationId = stakingId.integrationId,
                items = listOf(
                    balanceItem(type = BalanceType.STAKED, amount = staked),
                    balanceItem(type = BalanceType.REWARDS, amount = rewards),
                ),
            ),
        )
    }

    private fun balanceItem(type: BalanceType, amount: BigDecimal): BalanceItem = BalanceItem(
        groupId = "group",
        token = YieldToken.ETH,
        type = type,
        amount = amount,
        rawCurrencyId = null,
        validatorAddress = null,
        date = null,
        pendingActions = emptyList(),
        pendingActionsConstraints = emptyList(),
        isPending = false,
    )

    private fun availableOption(apy: BigDecimal): StakingAvailability.Available {
        val option = mockk<StakingOption>(relaxed = true) {
            every { this@mockk.apy } returns apy
        }
        return StakingAvailability.Available(option = option)
    }

    private fun fullOption(apy: BigDecimal): StakingAvailability.Full {
        val option = mockk<StakingOption>(relaxed = true) {
            every { this@mockk.apy } returns apy
        }
        return StakingAvailability.Full(option = option)
    }

    private fun buildStatusWithStake(stakedAmount: BigDecimal): CryptoCurrencyStatus {
        val network = mockk<Network>(relaxed = true) {
            every { rawId } returns "solana"
            every { isTestnet } returns false
        }
        val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { symbol } returns "SOL"
            every { decimals } returns 9
            every { this@mockk.network } returns network
            every { id.isCoin } returns true
        }
        val stakingBalance = mockk<StakingBalance.Data.P2PEthPool>(relaxed = true) {
            every { totalStaked } returns stakedAmount
            every { unstakingAmount } returns BigDecimal.ZERO
            every { withdrawableAmount } returns BigDecimal.ZERO
            every { totalRewards } returns BigDecimal.ZERO
        }
        val value = mockk<CryptoCurrencyStatus.Value>(relaxed = true) {
            every { this@mockk.stakingBalance } returns stakingBalance
            every { fiatRate } returns BigDecimal.ONE
            every { yieldSupplyStatus } returns null
        }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    private fun initialState(isBalanceHidden: Boolean = false): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = "Solana"),
            subtitle = stringReference("Solana network"),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = mockk<TokenDetailsBalanceBlockUM>(relaxed = true),
        notifications = persistentListOf(),
        earnBlockState = null,
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        pullToRefreshConfig = mockk<PullToRefreshConfig>(relaxed = true),
        isBalanceHidden = isBalanceHidden,
        isMarketPriceAvailable = false,
        addFundsUM = AddFundsUM.Loading,
        transferUM = TransferUM.Loading,
        zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
    )
}