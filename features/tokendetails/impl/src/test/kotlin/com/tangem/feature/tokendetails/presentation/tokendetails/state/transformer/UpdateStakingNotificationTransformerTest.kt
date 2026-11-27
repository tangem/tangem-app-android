package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.model.*
import com.tangem.domain.staking.model.BalanceType
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.THREE_STARS
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import com.tangem.core.res.R as CoreResR

class UpdateStakingNotificationTransformerTest {

    private val clickIntents: TokenDetailsClickIntents = mockk(relaxed = true)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EarnBannerShown {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Earn banner shown WHEN transform THEN no staking block`(model: StakingCase) {
            // Arrange
            val transformer = createTransformer(
                availability = model.availability,
                entryInfo = model.entryInfo,
                status = model.status,
                isEarnBannerShown = true,
            )

            // Act
            val result = transformer.transform(initialState())

            // Assert
            assertThat(result.earnBlockState).isNull()
        }

        private fun provideTestModels() = listOf(
            StakingCase(
                name = "stake offer",
                availability = availableOption(BigDecimal("4.2")),
                entryInfo = StakingEntryInfo(tokenSymbol = "ETH"),
                status = buildStatus(),
            ),
            StakingCase(
                name = "active stake",
                availability = availableOption(BigDecimal("4.2")),
                entryInfo = StakingEntryInfo(tokenSymbol = "ETH"),
                status = buildStatusWithStake(stakedAmount = BigDecimal("5")),
            ),
            StakingCase(
                name = "active stake in full pools",
                availability = fullOption(BigDecimal("4.2")),
                entryInfo = null,
                status = buildStatusWithStake(stakedAmount = BigDecimal("5")),
            ),
            StakingCase(
                name = "staking temporarily unavailable",
                availability = StakingAvailability.TemporaryUnavailable,
                entryInfo = null,
                status = buildStatus(),
            ),
        )
    }

    data class StakingCase(
        val name: String,
        val availability: StakingAvailability,
        val entryInfo: StakingEntryInfo?,
        val status: CryptoCurrencyStatus,
    ) {
        override fun toString(): String = name
    }

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

    @Test
    fun `GIVEN rewards to claim AND rate known WHEN transform THEN subtitle shows rate and reward amount`() {
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
        val combined = subtitle.text as TextReference.Combined
        val rateLabel = combined.refs.data.first() as TextReference.Combined
        val apyLabel = rateLabel.refs.data.filterIsInstance<TextReference.Res>().first()
        assertThat(apyLabel.id).isEqualTo(CoreResR.string.staking_details_apy)
        assertThat((combined.refs.data.last() as TextReference.Str).value).isNotEqualTo(THREE_STARS)
    }

    @Test
    fun `GIVEN auto-compound rewards AND rate known WHEN transform THEN subtitle shows rate only`() {
        // Arrange
        val status = buildStatus(
            networkRawId = "solana",
            symbol = "SOL",
            isCoin = true,
            stakingBalance = stakeKitBalance(staked = BigDecimal("100"), rewards = BigDecimal("5")),
        )
        val transformer = createTransformer(
            availability = availableOption(BigDecimal("4.2")),
            entryInfo = StakingEntryInfo(tokenSymbol = "SOL"),
            status = status,
            isBalanceHidden = false,
        )

        // Act
        val result = transformer.transform(initialState())

        // Assert
        val content = result.earnBlockState as EarnBlockUM.Content
        val subtitle = content.subtitleUM as EarnBlockUM.SubtitleUM.Text
        val rateLabel = subtitle.text as TextReference.Combined
        val apyLabel = rateLabel.refs.data.filterIsInstance<TextReference.Res>().first()
        assertThat(apyLabel.id).isEqualTo(CoreResR.string.staking_details_apy)
        assertThat(subtitle.tone).isEqualTo(EarnBlockUM.SubtitleUM.Tone.Accent)
    }

    @Test
    fun `GIVEN active stake at a validator below the best preferred rate WHEN transform THEN subtitle shows staked validator rate`() {
        // Arrange
        val stakedValidatorAddress = "pool_p2p"
        val status = buildStatus(
            networkRawId = "cardano",
            symbol = "ADA",
            isCoin = true,
            stakingBalance = stakeKitBalance(
                staked = BigDecimal("100"),
                rewards = BigDecimal("5"),
                validatorAddress = stakedValidatorAddress,
            ),
        )
        val option = stakeKitOption(
            validators = listOf(
                validator(address = "pool_other", apr = BigDecimal("0.0179")),
                validator(address = stakedValidatorAddress, apr = BigDecimal("0.0135")),
            ),
        )
        val transformer = createTransformer(
            availability = StakingAvailability.Available(option = option),
            entryInfo = StakingEntryInfo(tokenSymbol = "ADA"),
            status = status,
        )

        // Act
        val result = transformer.transform(initialState())

        // Assert
        val content = result.earnBlockState as EarnBlockUM.Content
        val subtitle = content.subtitleUM as EarnBlockUM.SubtitleUM.Text
        val rateLabel = (subtitle.text as TextReference.Combined).refs.data.first() as TextReference.Combined
        val rate = (rateLabel.refs.data.first() as TextReference.Str).value
        assertThat(rate).isEqualTo(BigDecimal("0.0135").format { percent() })
    }

    private fun stakeKitOption(validators: List<Yield.Validator>): StakingOption.StakeKit {
        val yield = mockk<Yield>(relaxed = true) {
            every { this@mockk.validators } returns validators
            every { preferredValidators } returns validators.filter { it.isPreferred }
        }
        return mockk(relaxed = true) {
            every { this@mockk.yield } returns yield
        }
    }

    private fun validator(address: String, apr: BigDecimal): Yield.Validator = Yield.Validator(
        address = address,
        status = Yield.Validator.ValidatorStatus.ACTIVE,
        name = address,
        rewardInfo = RewardInfo(rate = apr, type = RewardType.APR),
        isPreferred = true,
        isStrategicPartner = false,
    )

    private fun rewardFormatArg(text: TextReference): Any? = when (text) {
        is TextReference.Res -> text.formatArgs.data.firstOrNull()
        is TextReference.Combined -> (text.refs.data.last() as? TextReference.Str)?.value
        else -> null
    }

    private fun createTransformer(
        availability: StakingAvailability,
        entryInfo: StakingEntryInfo?,
        status: CryptoCurrencyStatus = buildStatus(),
        isBalanceHidden: Boolean = false,
        isEarnBannerShown: Boolean = false,
    ) = UpdateStakingNotificationTransformer(
        cryptoCurrencyStatus = status,
        stakingAvailability = availability,
        stakingEntryInfo = entryInfo,
        appCurrency = AppCurrency.Default,
        isBalanceHidden = isBalanceHidden,
        isEarnBannerShown = isEarnBannerShown,
        clickIntents = clickIntents,
    )

    private fun buildStatus(
        networkRawId: String = "solana",
        symbol: String = "SOL",
        isCoin: Boolean = true,
        stakingBalance: StakingBalance? = null,
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
            every { contributions } returns listOfNotNull(stakingBalance)
            every { fiatRate } returns BigDecimal.ONE
            every { yieldSupplyStatus } returns null
        }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    /** Builds an active StakeKit balance with the given [staked] and [rewards] amounts. */
    private fun stakeKitBalance(
        staked: BigDecimal,
        rewards: BigDecimal,
        validatorAddress: String? = null,
    ): StakingBalance.Data.StakeKit {
        val stakingId = StakingID(integrationId = "ethereum-eth-native-staking", address = "0xabc")
        return StakingBalance.Data.StakeKit(
            stakingId = stakingId,
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                integrationId = stakingId.integrationId,
                items = listOf(
                    balanceItem(type = BalanceType.STAKED, amount = staked, validatorAddress = validatorAddress),
                    balanceItem(type = BalanceType.REWARDS, amount = rewards, validatorAddress = validatorAddress),
                ),
            ),
        )
    }

    private fun balanceItem(
        type: BalanceType,
        amount: BigDecimal,
        validatorAddress: String? = null,
    ): BalanceItem = BalanceItem(
        groupId = "group",
        token = YieldToken.ETH,
        type = type,
        amount = amount,
        rawCurrencyId = null,
        validatorAddress = validatorAddress,
        date = null,
        pendingActions = emptyList(),
        pendingActionsConstraints = emptyList(),
        isPending = false,
    )

    private fun availableOption(apy: BigDecimal): StakingAvailability.Available {
        return StakingAvailability.Available(option = stakingOption(apy))
    }

    private fun fullOption(apy: BigDecimal): StakingAvailability.Full {
        return StakingAvailability.Full(option = stakingOption(apy))
    }

    /** Deterministic concrete [StakingOption] whose [displayRewardInfo] resolves to [apy] with an APY rate. */
    private fun stakingOption(apy: BigDecimal): StakingOption.P2PEthPool = mockk(relaxed = true) {
        every { this@mockk.apy } returns apy
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
            every { contributions } returns listOf(stakingBalance)
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