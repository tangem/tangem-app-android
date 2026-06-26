package com.tangem.domain.marketing

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.marketing.models.MarketingBanner
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingCampaignTarget
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.MarketingScreenType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMarketingBannerUseCaseTest {

    private val repository: MarketingRepository = mockk()
    private val featureToggles: MarketingFeatureToggles = mockk()
    private val useCase = GetMarketingBannerUseCase(repository, featureToggles)

    @BeforeEach
    fun reset() {
        clearMocks(repository, featureToggles)
        every { featureToggles.isMarketingBannersEnabled } returns true
        coEvery { repository.getDismissedBannerIds() } returns emptySet()
    }

    private fun banner() = MarketingBanner(
        uiType = MarketingBanner.UiType.STANDALONE, text = null, iconUrl = null,
        iconAlign = null, bgColor = null, deeplink = null, isDismissible = true,
    )

    private fun campaign(
        id: Int,
        type: MarketingScreenType,
        priority: Int,
        minAmount: BigDecimal? = null,
        maxAmount: BigDecimal? = null,
        targets: List<MarketingCampaignTarget> = emptyList(),
    ) = MarketingCampaign(id, type, priority, minAmount, maxAmount, providerIds = null, banner = banner(), targets = targets)

    private val swapScreen = MarketingScreen.Swap("eth", "0xF", "btc", "0xT")
    private val tokenScreen = MarketingScreen.TokenDetails(networkId = "ethereum", contractAddress = "0xA0b8")
    private val stakingScreen = MarketingScreen.Staking(networkId = "ethereum", contractAddress = "0xA0b8")
    private val yieldScreen = MarketingScreen.Yield(networkId = "ethereum", contractAddress = "0xA0b8")

    @Test
    fun `GIVEN toggle disabled WHEN invoke THEN empty without touching repository`() = runTest {
        // Arrange
        every { featureToggles.isMarketingBannersEnabled } returns false

        // Act
        val result = useCase(swapScreen)

        // Assert
        assertThat(result.getOrNull()).isEmpty()
        coVerify(exactly = 0) { repository.getCampaigns(any()) }
        coVerify(exactly = 0) { repository.getDismissedBannerIds() }
    }

    @Test
    fun `GIVEN several campaigns WHEN invoke THEN sorted by priority ascending`() = runTest {
        // Arrange
        coEvery { repository.getCampaigns(swapScreen) } returns listOf(
            campaign(id = 1, type = MarketingScreenType.SWAP, priority = 3),
            campaign(id = 2, type = MarketingScreenType.SWAP, priority = 1),
            campaign(id = 3, type = MarketingScreenType.SWAP, priority = 2),
        ).right()

        // Act
        val result = useCase(swapScreen)

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(2, 3, 1).inOrder()
    }

    @Test
    fun `GIVEN dismissed id WHEN invoke THEN dismissed campaign filtered out`() = runTest {
        // Arrange
        coEvery { repository.getDismissedBannerIds() } returns setOf(2)
        coEvery { repository.getCampaigns(swapScreen) } returns listOf(
            campaign(id = 1, type = MarketingScreenType.SWAP, priority = 1),
            campaign(id = 2, type = MarketingScreenType.SWAP, priority = 2),
        ).right()

        // Act
        val result = useCase(swapScreen)

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(1)
    }

    @Test
    fun `GIVEN amount below min WHEN invoke swap THEN campaign filtered out`() = runTest {
        // Arrange
        coEvery { repository.getCampaigns(swapScreen) } returns listOf(
            campaign(id = 1, type = MarketingScreenType.SWAP, priority = 1, minAmount = BigDecimal(50), maxAmount = BigDecimal(300)),
        ).right()

        // Act
        val result = useCase(swapScreen, amountUsd = BigDecimal(25))

        // Assert
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `GIVEN amount within range WHEN invoke swap THEN campaign kept`() = runTest {
        // Arrange
        coEvery { repository.getCampaigns(swapScreen) } returns listOf(
            campaign(id = 1, type = MarketingScreenType.SWAP, priority = 1, minAmount = BigDecimal(50), maxAmount = BigDecimal(300)),
        ).right()

        // Act
        val result = useCase(swapScreen, amountUsd = BigDecimal(100))

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(1)
    }

    @Test
    fun `GIVEN amount above max WHEN invoke swap THEN campaign filtered out`() = runTest {
        // Arrange
        coEvery { repository.getCampaigns(swapScreen) } returns listOf(
            campaign(id = 1, type = MarketingScreenType.SWAP, priority = 1, minAmount = BigDecimal(50), maxAmount = BigDecimal(300)),
        ).right()

        // Act
        val result = useCase(swapScreen, amountUsd = BigDecimal(500))

        // Assert
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `GIVEN null amount WHEN invoke swap THEN amount filter skipped`() = runTest {
        // Arrange
        coEvery { repository.getCampaigns(swapScreen) } returns listOf(
            campaign(id = 1, type = MarketingScreenType.SWAP, priority = 1, minAmount = BigDecimal(50)),
        ).right()

        // Act
        val result = useCase(swapScreen, amountUsd = null)

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(1)
    }

    @Test
    fun `GIVEN background type WHEN invoke THEN only campaigns matching the on-screen token kept`() = runTest {
        // Arrange
        coEvery { repository.getCampaigns(tokenScreen) } returns listOf(
            campaign(
                id = 1, type = MarketingScreenType.TOKEN_DETAILS, priority = 1,
                targets = listOf(MarketingCampaignTarget.NetworkContract("ethereum", "0xA0b8")),
            ),
            campaign(
                id = 2, type = MarketingScreenType.TOKEN_DETAILS, priority = 2,
                targets = listOf(MarketingCampaignTarget.NetworkContract("bitcoin", "0xOther")),
            ),
        ).right()

        // Act
        val result = useCase(tokenScreen)

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(1)
    }

    @Test
    fun `GIVEN token markets screen WHEN invoke THEN only campaigns matching the coingecko id kept`() = runTest {
        // Arrange
        val marketsScreen = MarketingScreen.TokenMarkets(coingeckoId = "1696501400")
        coEvery { repository.getCampaigns(marketsScreen) } returns listOf(
            campaign(
                id = 1, type = MarketingScreenType.TOKEN_MARKETS, priority = 1,
                targets = listOf(MarketingCampaignTarget.CoingeckoId("1696501400")),
            ),
            campaign(
                id = 2, type = MarketingScreenType.TOKEN_MARKETS, priority = 2,
                targets = listOf(MarketingCampaignTarget.CoingeckoId("other")),
            ),
        ).right()

        // Act
        val result = useCase(marketsScreen)

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(1)
    }

    @Test
    fun `GIVEN staking screen WHEN invoke THEN only campaigns matching the on-screen token kept`() = runTest {
        // Arrange
        coEvery { repository.getCampaigns(stakingScreen) } returns listOf(
            campaign(
                id = 1, type = MarketingScreenType.STAKING, priority = 1,
                targets = listOf(MarketingCampaignTarget.NetworkContract("ethereum", "0xA0b8")),
            ),
            campaign(
                id = 2, type = MarketingScreenType.STAKING, priority = 2,
                targets = listOf(MarketingCampaignTarget.NetworkContract("bitcoin", "0xOther")),
            ),
        ).right()

        // Act
        val result = useCase(stakingScreen)

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(1)
    }

    @Test
    fun `GIVEN yield screen WHEN invoke THEN only campaigns matching the on-screen token kept`() = runTest {
        // Arrange
        coEvery { repository.getCampaigns(yieldScreen) } returns listOf(
            campaign(
                id = 1, type = MarketingScreenType.YIELD, priority = 1,
                targets = listOf(MarketingCampaignTarget.NetworkContract("ethereum", "0xA0b8")),
            ),
            campaign(
                id = 2, type = MarketingScreenType.YIELD, priority = 2,
                targets = listOf(MarketingCampaignTarget.NetworkContract("bitcoin", "0xOther")),
            ),
        ).right()

        // Act
        val result = useCase(yieldScreen)

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(1)
    }

    @Test
    fun `GIVEN contract address differing only in case WHEN invoke THEN campaign matched`() = runTest {
        // Arrange
        val screen = MarketingScreen.TokenDetails(networkId = "ethereum", contractAddress = "0xA0B8")
        coEvery { repository.getCampaigns(screen) } returns listOf(
            campaign(
                id = 1, type = MarketingScreenType.TOKEN_DETAILS, priority = 1,
                targets = listOf(MarketingCampaignTarget.NetworkContract("ethereum", "0xa0b8")),
            ),
        ).right()

        // Act
        val result = useCase(screen)

        // Assert
        assertThat(result.getOrNull()?.map { it.id }).containsExactly(1)
    }
}