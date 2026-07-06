package com.tangem.data.yield.supply.promo

import com.google.common.truth.Truth.assertThat
import com.tangem.data.yield.supply.promo.converter.YieldBoostPromoConverter
import com.tangem.data.yield.supply.promo.converter.YieldBoostStatusConverter
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.datasource.api.promotion.models.YieldBoostStatusResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.yieldsupply.promo.YieldBoostPromoStore
import com.tangem.datasource.local.yieldsupply.promo.YieldBoostStatusStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultYieldPromoRepositoryTest {

    private val tangemApi: TangemTechApi = mockk()
    private val promoStore: YieldBoostPromoStore = mockk(relaxed = true)
    private val statusStore: YieldBoostStatusStore = mockk(relaxed = true)

    private val repository = DefaultYieldPromoRepository(
        tangemApi = tangemApi,
        promoStore = promoStore,
        statusStore = statusStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("abcdef012345")

    @BeforeEach
    fun setUp() {
        clearMocks(tangemApi, promoStore, statusStore)
    }

    // region getYieldBoostPromo
    @Test
    fun `GIVEN cached promo and no refresh WHEN getYieldBoostPromo THEN returns cache without api`() = runTest {
        // Arrange
        val cached = YieldBoostPromo.None
        coEvery { promoStore.getSyncOrNull(userWalletId) } returns cached

        // Act
        val result = repository.getYieldBoostPromo(userWalletId, forceRefresh = false)

        // Assert
        assertThat(result).isEqualTo(cached)
        coVerify(exactly = 0) { tangemApi.getPromotions(any(), any()) }
    }

    @Test
    fun `GIVEN no cache WHEN getYieldBoostPromo THEN fetches stores and returns converted`() = runTest {
        // Arrange
        val dto = matchingPromoDto()
        coEvery { promoStore.getSyncOrNull(userWalletId) } returns null
        coEvery { tangemApi.getPromotions(any(), any()) } returns ApiResponse.Success(
            PromotionsResponse(promotions = listOf(dto)),
        )
        val expected = YieldBoostPromoConverter.convert(dto)

        // Act
        val result = repository.getYieldBoostPromo(userWalletId, forceRefresh = false)

        // Assert
        assertThat(result).isEqualTo(expected)
        coVerify(exactly = 1) { promoStore.store(userWalletId, expected) }
    }

    @Test
    fun `GIVEN cached promo and force refresh WHEN getYieldBoostPromo THEN fetches anyway`() = runTest {
        // Arrange
        coEvery { promoStore.getSyncOrNull(userWalletId) } returns YieldBoostPromo.None
        coEvery { tangemApi.getPromotions(any(), any()) } returns ApiResponse.Success(
            PromotionsResponse(promotions = listOf(matchingPromoDto())),
        )

        // Act
        repository.getYieldBoostPromo(userWalletId, forceRefresh = true)

        // Assert
        coVerify(exactly = 1) { tangemApi.getPromotions(any(), any()) }
    }

    @Test
    fun `GIVEN no matching promo name WHEN getYieldBoostPromo THEN returns None`() = runTest {
        // Arrange
        coEvery { promoStore.getSyncOrNull(userWalletId) } returns null
        coEvery { tangemApi.getPromotions(any(), any()) } returns ApiResponse.Success(
            PromotionsResponse(promotions = listOf(PromotionsResponse.PromotionDto(name = "other", all = null))),
        )

        // Act
        val result = repository.getYieldBoostPromo(userWalletId, forceRefresh = false)

        // Assert
        assertThat(result).isEqualTo(YieldBoostPromo.None)
        coVerify(exactly = 1) { promoStore.store(userWalletId, YieldBoostPromo.None) }
    }

    @Test
    fun `GIVEN fetch fails and cache present WHEN getYieldBoostPromo THEN falls back to cache`() = runTest {
        // Arrange — force refresh so the initial cache check is skipped and the fetch is attempted
        val cached = YieldBoostPromo.None
        coEvery { tangemApi.getPromotions(any(), any()) } throws IOException("network")
        coEvery { promoStore.getSyncOrNull(userWalletId) } returns cached

        // Act
        val result = repository.getYieldBoostPromo(userWalletId, forceRefresh = true)

        // Assert
        assertThat(result).isEqualTo(cached)
        coVerify(exactly = 0) { promoStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN fetch fails and no cache WHEN getYieldBoostPromo THEN rethrows`() = runTest {
        // Arrange
        coEvery { tangemApi.getPromotions(any(), any()) } throws IOException("network")
        coEvery { promoStore.getSyncOrNull(userWalletId) } returns null

        // Act
        val error = runCatching { repository.getYieldBoostPromo(userWalletId, forceRefresh = true) }
            .exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IOException::class.java)
    }
    // endregion

    // region getYieldBoostStatus
    @Test
    fun `GIVEN cached status and no refresh WHEN getYieldBoostStatus THEN returns cache without api`() = runTest {
        // Arrange
        val cached = YieldBoostStatus.NotStarted
        coEvery { statusStore.getSyncOrNull(userWalletId) } returns cached

        // Act
        val result = repository.getYieldBoostStatus(userWalletId, forceRefresh = false)

        // Assert
        assertThat(result).isEqualTo(cached)
        coVerify(exactly = 0) { tangemApi.getYieldBoostStatus(any()) }
    }

    @Test
    fun `GIVEN no cache WHEN getYieldBoostStatus THEN fetches stores and returns converted`() = runTest {
        // Arrange
        val response = statusResponse()
        coEvery { statusStore.getSyncOrNull(userWalletId) } returns null
        coEvery { tangemApi.getYieldBoostStatus(any()) } returns ApiResponse.Success(response)
        val expected = YieldBoostStatusConverter.convert(response)

        // Act
        val result = repository.getYieldBoostStatus(userWalletId, forceRefresh = false)

        // Assert
        assertThat(result).isEqualTo(expected)
        coVerify(exactly = 1) { statusStore.store(userWalletId, expected) }
    }

    @Test
    fun `GIVEN cached status and force refresh WHEN getYieldBoostStatus THEN fetches anyway`() = runTest {
        // Arrange
        coEvery { statusStore.getSyncOrNull(userWalletId) } returns YieldBoostStatus.NotStarted
        coEvery { tangemApi.getYieldBoostStatus(any()) } returns ApiResponse.Success(statusResponse())

        // Act
        repository.getYieldBoostStatus(userWalletId, forceRefresh = true)

        // Assert
        coVerify(exactly = 1) { tangemApi.getYieldBoostStatus(any()) }
    }

    @Test
    fun `GIVEN fetch fails and cache present WHEN getYieldBoostStatus THEN falls back to cache`() = runTest {
        // Arrange — force refresh so the initial cache check is skipped and the fetch is attempted
        val cached = YieldBoostStatus.NotStarted
        coEvery { tangemApi.getYieldBoostStatus(any()) } throws IOException("network")
        coEvery { statusStore.getSyncOrNull(userWalletId) } returns cached

        // Act
        val result = repository.getYieldBoostStatus(userWalletId, forceRefresh = true)

        // Assert
        assertThat(result).isEqualTo(cached)
        coVerify(exactly = 0) { statusStore.store(any(), any()) }
    }

    @Test
    fun `GIVEN fetch fails and no cache WHEN getYieldBoostStatus THEN rethrows`() = runTest {
        // Arrange
        coEvery { tangemApi.getYieldBoostStatus(any()) } throws IOException("network")
        coEvery { statusStore.getSyncOrNull(userWalletId) } returns null

        // Act
        val error = runCatching { repository.getYieldBoostStatus(userWalletId, forceRefresh = true) }
            .exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IOException::class.java)
    }
    // endregion

    private fun matchingPromoDto() = PromotionsResponse.PromotionDto(
        name = "yield-apr-boost",
        all = PromotionsResponse.PromotionDto.All(
            timeline = PromotionsResponse.PromotionDto.Timeline(
                start = "2026-06-15T00:00:00.000Z",
                end = "2027-06-15T22:00:00.000Z",
            ),
            tokens = listOf(
                PromotionsResponse.PromotionDto.PromoToken(
                    tokenAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
                    tokenSymbol = "USDC",
                    tokenName = "USD Coin",
                    networkId = "ethereum",
                ),
            ),
            status = "active",
            link = "https://example.com/terms",
        ),
    )

    private fun statusResponse() = YieldBoostStatusResponse(
        tokenName = "USD Coin",
        networkId = "ethereum",
        moduleAddress = "0xModule",
        userAddress = "0xUser",
        contractAddress = "0xContract",
        promoEnrollmentStatus = "NOT_STARTED",
        qualificationEndDate = null,
        disqualificationReason = null,
    )
}