package com.tangem.features.yield.supply.impl.incentives.banner.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMarketFlowUseCase
import com.tangem.features.yield.supply.impl.incentives.banner.DefaultYieldSupplyRewardsBannerComponent
import com.tangem.features.yield.supply.impl.incentives.banner.entity.YieldSupplyRewardsBannerUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.yield.MockYieldSupply
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyRewardsBannerModelTest {

    private val marketFlowUseCase: YieldSupplyMarketFlowUseCase = mockk()
    private val getRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase = mockk()

    private var originalLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(marketFlowUseCase, getRewardsSummaryUseCase)
        every { marketFlowUseCase(any()) } returns flowOf(market(hasCampaign = true))
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN recommended rewards WHEN model created THEN banner shows them with the claim CTA inert`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns
            YieldRewardsSummary.Claimable(
                moduleAddress = MODULE_ADDRESS,
                claimable = listOf(reward(claimable = "1000")),
                pending = emptyList(),
                totalNetUsd = BigDecimal("30.00"),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(
            YieldSupplyRewardsBannerUM(
                title = resourceReference(R.string.yield_module_rewards_banner_title, wrappedList("\$30.00")),
                description = resourceReference(R.string.yield_module_rewards_banner_subtitle),
                claim = YieldSupplyRewardsBannerUM.Claim.Disabled,
            ),
        )

        model.onDestroy()
    }

    @Test
    fun `GIVEN rewards below threshold WHEN model created THEN claim is disabled`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns
            YieldRewardsSummary.Deferred(
                moduleAddress = MODULE_ADDRESS,
                claimable = listOf(reward(claimable = "1000")),
                pending = emptyList(),
                totalNetUsd = BigDecimal("0.30"),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(
            YieldSupplyRewardsBannerUM(
                title = resourceReference(R.string.yield_module_rewards_banner_title, wrappedList("\$0.30")),
                description = resourceReference(R.string.yield_module_rewards_banner_below_threshold_subtitle),
                claim = YieldSupplyRewardsBannerUM.Claim.Disabled,
            ),
        )

        model.onDestroy()
    }

    @Test
    fun `GIVEN only pending rewards WHEN model created THEN banner lists amounts without claim`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns
            YieldRewardsSummary.PendingOnly(
                pending = listOf(
                    reward(symbol = "WETH", pending = "1500000000000000000"),
                    reward(symbol = "AAVE", pending = "2250000000000000000"),
                ),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(
            YieldSupplyRewardsBannerUM(
                title = resourceReference(R.string.yield_module_rewards_banner_pending_title),
                description = resourceReference(
                    id = R.string.yield_module_rewards_banner_pending_subtitle,
                    formatArgs = wrappedList("WETH${NON_BREAKING_SPACE}1.50, AAVE${NON_BREAKING_SPACE}2.25"),
                ),
                claim = YieldSupplyRewardsBannerUM.Claim.Hidden,
            ),
        )

        model.onDestroy()
    }

    @Test
    fun `GIVEN pending reward below the display cap WHEN model created THEN amount keeps its lower sign`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns
            YieldRewardsSummary.PendingOnly(pending = listOf(reward(symbol = "WETH", pending = "1"))).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isEqualTo(
            YieldSupplyRewardsBannerUM(
                title = resourceReference(R.string.yield_module_rewards_banner_pending_title),
                description = resourceReference(
                    id = R.string.yield_module_rewards_banner_pending_subtitle,
                    formatArgs = wrappedList("<${NON_BREAKING_SPACE}WETH${NON_BREAKING_SPACE}0.00000001"),
                ),
                claim = YieldSupplyRewardsBannerUM.Claim.Hidden,
            ),
        )

        model.onDestroy()
    }

    @Test
    fun `GIVEN nothing to surface WHEN model created THEN no banner`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns
            YieldRewardsSummary.None.right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isNull()

        model.onDestroy()
    }

    @Test
    fun `GIVEN no running campaign WHEN model created THEN rewards are not requested`() = runTest {
        // Arrange
        every { marketFlowUseCase(any()) } returns flowOf(market(hasCampaign = false))

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isNull()
        coVerify(exactly = 0) { getRewardsSummaryUseCase(any(), any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN incentives unavailable WHEN model created THEN no banner`() = runTest {
        // Arrange
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns
            YieldIncentivesError.Unavailable.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isNull()

        model.onDestroy()
    }

    @Test
    fun `GIVEN address not resolved yet WHEN model created THEN rewards are not requested`() = runTest {
        // Arrange
        val statusFlow = MutableStateFlow(
            CryptoCurrencyStatus(currency = createToken(), value = CryptoCurrencyStatus.Loading),
        )

        // Act
        val model = createModel(cryptoCurrencyStatusFlow = statusFlow)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isNull()
        coVerify(exactly = 0) { getRewardsSummaryUseCase(any(), any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN markets cache warms up after being empty WHEN collected THEN the banner appears`() = runTest {
        // Arrange
        every { marketFlowUseCase(any()) } returns flowOf(null, market(hasCampaign = true))
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns
            YieldRewardsSummary.Claimable(
                moduleAddress = MODULE_ADDRESS,
                claimable = listOf(reward(claimable = "1000")),
                pending = emptyList(),
                totalNetUsd = BigDecimal("30.00"),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value?.claim).isEqualTo(YieldSupplyRewardsBannerUM.Claim.Disabled)

        model.onDestroy()
    }

    @Test
    fun `GIVEN the campaign ends while the screen is open WHEN collected THEN the banner disappears`() = runTest {
        // Arrange
        every { marketFlowUseCase(any()) } returns
            flowOf(market(hasCampaign = true), market(hasCampaign = false))
        coEvery { getRewardsSummaryUseCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS) } returns
            YieldRewardsSummary.Claimable(
                moduleAddress = MODULE_ADDRESS,
                claimable = listOf(reward(claimable = "1000")),
                pending = emptyList(),
                totalNetUsd = BigDecimal("30.00"),
            ).right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isNull()

        model.onDestroy()
    }

    private fun TestScope.createModel(
        cryptoCurrencyStatusFlow: MutableStateFlow<CryptoCurrencyStatus> = MutableStateFlow(loadedStatus()),
    ) = YieldSupplyRewardsBannerModel(
        paramsContainer = MutableParamsContainer(
            DefaultYieldSupplyRewardsBannerComponent.Params(
                cryptoCurrency = createToken(),
                cryptoCurrencyStatusFlow = cryptoCurrencyStatusFlow,
                onClaimClick = {},
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        yieldSupplyMarketFlowUseCase = marketFlowUseCase,
        getYieldRewardsSummaryUseCase = getRewardsSummaryUseCase,
    )

    private fun loadedStatus() = CryptoCurrencyStatus(
        currency = createToken(),
        value = CryptoCurrencyStatus.NoQuote(
            amount = BigDecimal.TEN,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            yieldSupplyStatus = null,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = USER_ADDRESS,
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private fun market(hasCampaign: Boolean) = MockYieldSupply.market(
        tokenAddress = TOKEN_ADDRESS,
        chainId = CHAIN_ID,
        apy = BigDecimal("4.0"),
        campaign = MockYieldSupply.campaign(hasActiveCampaign = hasCampaign),
    )

    private fun reward(
        symbol: String = "WETH",
        claimable: String = "0",
        pending: String = "0",
    ) = MockYieldSupply.reward(
        rewardToken = "0x$symbol",
        symbol = symbol,
        claimableAmount = BigInteger(claimable),
        pendingAmount = BigInteger(pending),
        usdValue = BigDecimal.ZERO,
        estimatedNetValueUsd = BigDecimal.ZERO,
        estimatedServiceFeeUsd = BigDecimal.ZERO,
    )

    private fun createToken(): CryptoCurrency.Token = MockYieldSupply.token(contractAddress = TOKEN_ADDRESS)

    private companion object {
        const val CHAIN_ID = 1
        const val TOKEN_ADDRESS = "0xToken"
        const val USER_ADDRESS = "0xUser"
        const val MODULE_ADDRESS = "0xModule"
    }
}