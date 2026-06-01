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
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.StakingOption
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

    private fun createTransformer(
        availability: StakingAvailability,
        entryInfo: StakingEntryInfo?,
    ) = UpdateStakingNotificationTransformer(
        cryptoCurrencyStatus = buildStatus(),
        stakingAvailability = availability,
        stakingEntryInfo = entryInfo,
        appCurrency = AppCurrency.Default,
        clickIntents = clickIntents,
    )

    private fun buildStatus(): CryptoCurrencyStatus {
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
        val stakingBalance = mockk<StakingBalance>(relaxed = true)
        val value = mockk<CryptoCurrencyStatus.Value>(relaxed = true) {
            every { this@mockk.stakingBalance } returns stakingBalance
            every { fiatRate } returns BigDecimal.ONE
            every { yieldSupplyStatus } returns null
        }
        return CryptoCurrencyStatus(currency = currency, value = value)
    }

    private fun availableOption(apy: BigDecimal): StakingAvailability.Available {
        val option = mockk<StakingOption>(relaxed = true) {
            every { this@mockk.apy } returns apy
        }
        return StakingAvailability.Available(option = option)
    }

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
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
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
        addFundsUM = AddFundsUM.Loading,
        transferUM = TransferUM.Loading,
        zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
    )
}