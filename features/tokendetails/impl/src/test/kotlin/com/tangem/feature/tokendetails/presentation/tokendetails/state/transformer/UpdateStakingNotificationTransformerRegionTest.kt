package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class UpdateStakingNotificationTransformerRegionTest {

    private val clickIntents: TokenDetailsClickIntents = mockk(relaxed = true)

    @Test
    fun `GIVEN region unavailable AND staked WHEN transform THEN tappable region block`() {
        // Arrange
        val status = buildStatusWithStake(stakedAmount = BigDecimal("5"))
        val transformer = UpdateStakingNotificationTransformer(
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.RegionUnavailable,
            stakingEntryInfo = null,
            appCurrency = AppCurrency.Default,
            isBalanceHidden = false,
            clickIntents = clickIntents,
        )

        // Act
        val result = transformer.transform(initialState()).earnBlockState

        // Assert
        val content = result as EarnBlockUM.Content
        assertThat(content.onClick).isNotNull()
        assertThat(content.trailingUM).isNull()
        assertThat(content.subtitleUM).isInstanceOf(EarnBlockUM.SubtitleUM.Text::class.java)
    }

    @Test
    fun `GIVEN region unavailable AND not staked WHEN transform THEN no block`() {
        // Arrange
        val status = buildStatus()
        val transformer = UpdateStakingNotificationTransformer(
            cryptoCurrencyStatus = status,
            stakingAvailability = StakingAvailability.RegionUnavailable,
            stakingEntryInfo = null,
            appCurrency = AppCurrency.Default,
            isBalanceHidden = false,
            clickIntents = clickIntents,
        )

        // Act
        val result = transformer.transform(initialState()).earnBlockState

        // Assert
        assertThat(result).isNull()
    }

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

    private fun buildStatusWithStake(stakedAmount: BigDecimal): CryptoCurrencyStatus {
        val network = mockk<Network>(relaxed = true) {
            every { rawId } returns "ethereum"
            every { isTestnet } returns false
        }
        val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { symbol } returns "ETH"
            every { decimals } returns 18
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

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = "Ethereum"),
            subtitle = stringReference("Ethereum network"),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = mockk<TokenDetailsBalanceBlockUM>(relaxed = true),
        notifications = persistentListOf(),
        earnBlockState = null,
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        pullToRefreshConfig = mockk<PullToRefreshConfig>(relaxed = true),
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
        addFundsUM = AddFundsUM.Loading,
        transferUM = TransferUM.Loading,
        zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
    )
}