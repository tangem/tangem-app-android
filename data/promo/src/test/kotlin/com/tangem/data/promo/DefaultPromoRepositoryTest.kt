package com.tangem.data.promo

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
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
import com.tangem.domain.promo.models.EnrolledTokenReward
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState
import com.tangem.domain.promo.models.TokenReward
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPromoRepositoryTest {

    private val promotionsSupplier: PromotionsSupplier = mockk()
    private val tangemApi: TangemTechApi = mockk()
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val repository = DefaultPromoRepository(
        promotionsSupplier = promotionsSupplier,
        tangemApi = tangemApi,
        moshi = moshi,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val campaign = PromoCampaignId.WhaleSwapCashback
    private val userWalletId = UserWalletId("abcdef012345")
    private val tokenReward = TokenReward("0xToken", "ethereum", "0xUser", "tether")

    // The enroll result drops userAddress — this is what the submitted tokenReward collapses to.
    private val resultTokenReward = EnrolledTokenReward("0xToken", "ethereum", "tether")

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
    fun setUp() = clearMocks(promotionsSupplier, tangemApi)

    @Test
    fun `GIVEN active campaign present and not enrolled WHEN getCampaignState THEN Available`() = runTest {
        // Arrange
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
        val finished = activeDto().copy(all = activeDto().all!!.copy(status = "finished"))
        coEvery { promotionsSupplier.getPromotions(userWalletId, any()) } returns
            PromotionsResponse(promotions = listOf(finished))

        // Act
        val result = repository.getCampaignState(campaign, userWalletId)

        // Assert
        assertThat(result).isEqualTo(PromoCampaignState.NotActive(campaign))
    }

    @Test
    fun `GIVEN api returns 201 with canonical token WHEN enroll THEN Success with backend token`() = runTest {
        // Arrange
        val data = PromotionRegistrationResponse.RegistrationData(
            campaignId = campaign.slug,
            registeredAt = "2026-07-06T09:27:13.363Z",
            tokenReward = PromotionRegistrationResponse.RegisteredTokenRewardDto(
                tokenAddress = "0xCanonical",
                networkId = "ethereum",
                tokenId = "tether",
            ),
        )
        coEvery { tangemApi.createPromotionRegistration(any()) } returns ApiResponse.Success(
            PromotionRegistrationResponse(status = "saved", message = null, data = data),
        )

        // Act
        val result = repository.enroll(campaign, tokenReward, listOf(userWalletId))

        // Assert
        val backendToken = EnrolledTokenReward("0xCanonical", "ethereum", "tether")
        assertThat(result).isEqualTo(EnrollResult.Success(backendToken))
    }

    @Test
    fun `GIVEN api returns 409 WHEN enroll THEN AlreadyEnrolled with existing token`() = runTest {
        // Arrange
        val existing = """
            {"status":"already_exists","message":"exists","data":{"campaignId":"${campaign.slug}",
            "registeredAt":"2026-07-01T10:00:00.000Z","tokenReward":{"tokenAddress":"0xOther",
            "networkId":"base","userAddress":"0xExisting","tokenId":"usd-coin"}}}
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
        val expectedToken = EnrolledTokenReward("0xOther", "base", "usd-coin")
        assertThat(result).isEqualTo(EnrollResult.AlreadyEnrolled(expectedToken))
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
        assertThat(result).isEqualTo(EnrollResult.AlreadyEnrolled(resultTokenReward))
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
    }
}