package com.tangem.data.promo

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.data.promo.store.PromoEnrollmentStore
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.promotion.models.CreatePromotionRegistrationBody
import com.tangem.datasource.api.promotion.models.PromotionRegistrationResponse
import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto.All
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto.PromoToken
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto.Timeline
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.promotion.PromotionsSupplier
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.EnrollResult
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.models.TokenReward
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPromoRepositoryTest {

    private val promotionsSupplier: PromotionsSupplier = mockk()
    private val tangemApi: TangemTechApi = mockk()
    private val enrollmentStore: PromoEnrollmentStore = mockk(relaxed = true)
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val repository = DefaultPromoRepository(
        promotionsSupplier = promotionsSupplier,
        tangemApi = tangemApi,
        enrollmentStore = enrollmentStore,
        moshi = moshi,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val campaign = PromoCampaignId.WhaleSwapCashback
    private val userWalletId = UserWalletId("abcdef012345")
    private val tokenReward = TokenReward("0xToken", "ethereum")

    private fun activeDto() = PromotionDto(
        name = campaign.slug,
        all = All(
            timeline = Timeline("2026-06-23T00:00:00.000Z", "2026-08-31T20:59:59.000Z"),
            tokens = listOf(
                PromoToken(
                    tokenId = "tether",
                    tokenAddress = "0xToken",
                    tokenSymbol = "USDT",
                    tokenName = "Tether USD",
                    networkId = "ethereum",
                    decimals = 6,
                ),
            ),
            status = "active",
            link = "",
        ),
    )

    @BeforeEach
    fun setUp() = clearMocks(promotionsSupplier, tangemApi, enrollmentStore)

    @Test
    fun `GIVEN locally enrolled WHEN getCampaignState THEN Enrolled without api`() = runTest {
        // Arrange
        coEvery { enrollmentStore.getSyncOrNull(campaign) } returns tokenReward

        // Act
        val result = repository.getCampaignState(campaign, userWalletId)

        // Assert
        assertThat(result).isEqualTo(PromoCampaignState.Enrolled(campaign, tokenReward))
        coVerify(exactly = 0) { promotionsSupplier.getPromotions(any(), any()) }
    }

    @Test
    fun `GIVEN active campaign present and not enrolled WHEN getCampaignState THEN Available`() = runTest {
        // Arrange
        coEvery { enrollmentStore.getSyncOrNull(campaign) } returns null
        coEvery { promotionsSupplier.getPromotions(userWalletId, any()) } returns
            PromotionsResponse(promotions = listOf(activeDto()))

        // Act
        val result = repository.getCampaignState(campaign, userWalletId)

        // Assert
        assertThat(result).isInstanceOf(PromoCampaignState.Available::class.java)
    }

    @Test
    fun `GIVEN campaign absent WHEN getCampaignState THEN NotActive`() = runTest {
        // Arrange
        coEvery { enrollmentStore.getSyncOrNull(campaign) } returns null
        coEvery { promotionsSupplier.getPromotions(userWalletId, any()) } returns
            PromotionsResponse(promotions = emptyList())

        // Act
        val result = repository.getCampaignState(campaign, userWalletId)

        // Assert
        assertThat(result).isEqualTo(PromoCampaignState.NotActive(campaign))
    }

    @Test
    fun `GIVEN campaign present but finished WHEN getCampaignState THEN NotActive`() = runTest {
        // Arrange
        coEvery { enrollmentStore.getSyncOrNull(campaign) } returns null
        val finished = activeDto().copy(all = activeDto().all!!.copy(status = "finished"))
        coEvery { promotionsSupplier.getPromotions(userWalletId, any()) } returns
            PromotionsResponse(promotions = listOf(finished))

        // Act
        val result = repository.getCampaignState(campaign, userWalletId)

        // Assert
        assertThat(result).isEqualTo(PromoCampaignState.NotActive(campaign))
    }

    @Test
    fun `GIVEN api returns 201 with canonical token WHEN enroll THEN Success and persists backend token`() = runTest {
        // Arrange
        val data = PromotionRegistrationResponse.RegistrationData(
            campaignId = campaign.slug,
            registeredAt = "2026-07-06T09:27:13.363Z",
            tokenReward = CreatePromotionRegistrationBody.TokenRewardDto("0xCanonical", "ethereum"),
        )
        coEvery { tangemApi.createPromotionRegistration(any()) } returns ApiResponse.Success(
            PromotionRegistrationResponse(status = "saved", message = null, data = data),
        )

        // Act
        val result = repository.enroll(campaign, tokenReward, listOf(userWalletId))

        // Assert
        val backendToken = TokenReward("0xCanonical", "ethereum")
        assertThat(result).isEqualTo(EnrollResult.Success(backendToken))
        coVerify(exactly = 1) { enrollmentStore.store(campaign, backendToken) }
    }

    @Test
    fun `GIVEN api returns 409 WHEN enroll THEN AlreadyEnrolled with existing token`() = runTest {
        // Arrange
        val existing = """
            {"status":"already_exists","message":"exists","data":{"campaignId":"${campaign.slug}",
            "registeredAt":"2026-07-01T10:00:00.000Z","tokenReward":{"tokenAddress":"0xOther",
            "networkId":"base","userAddress":"0xExisting"}}}
        """.trimIndent()
        @Suppress("UNCHECKED_CAST")
        coEvery { tangemApi.createPromotionRegistration(any()) } returns ApiResponse.Error(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.CONFLICT,
                message = "conflict",
                errorBody = existing,
            ),
        ) as ApiResponse<PromotionRegistrationResponse>

        // Act
        val result = repository.enroll(campaign, tokenReward, listOf(userWalletId))

        // Assert
        val expectedToken = TokenReward("0xOther", "base")
        assertThat(result).isEqualTo(EnrollResult.AlreadyEnrolled(expectedToken))
        coVerify(exactly = 1) { enrollmentStore.store(campaign, expectedToken) }
    }

    @Test
    fun `GIVEN 409 with null errorBody WHEN enroll THEN AlreadyEnrolled with submitted token`() = runTest {
        // Arrange
        @Suppress("UNCHECKED_CAST")
        coEvery { tangemApi.createPromotionRegistration(any()) } returns ApiResponse.Error(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.CONFLICT,
                message = "conflict",
                errorBody = null,
            ),
        ) as ApiResponse<PromotionRegistrationResponse>

        // Act
        val result = repository.enroll(campaign, tokenReward, listOf(userWalletId))

        // Assert
        assertThat(result).isEqualTo(EnrollResult.AlreadyEnrolled(tokenReward))
        coVerify(exactly = 1) { enrollmentStore.store(campaign, tokenReward) }
    }

    @Test
    fun `GIVEN api returns 500 WHEN enroll THEN throws`() = runTest {
        // Arrange
        @Suppress("UNCHECKED_CAST")
        coEvery { tangemApi.createPromotionRegistration(any()) } returns ApiResponse.Error(
            ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
                message = "server",
                errorBody = null,
            ),
        ) as ApiResponse<PromotionRegistrationResponse>

        // Act
        val error = runCatching { repository.enroll(campaign, tokenReward, listOf(userWalletId)) }.exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
        coVerify(exactly = 0) { enrollmentStore.store(any(), any()) }
    }
}