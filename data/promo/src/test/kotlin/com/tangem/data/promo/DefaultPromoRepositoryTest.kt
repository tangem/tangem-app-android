package com.tangem.data.promo

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.promotion.models.PromotionRegistrationResponse
import com.tangem.datasource.api.promotion.models.PromotionsResponse
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto
import com.tangem.datasource.api.promotion.models.PromotionsResponse.PromotionDto.*
import com.tangem.datasource.api.promotion.models.PromotionWalletType
import com.tangem.datasource.api.promotion.models.PromotionsV3Response
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.promotion.PromotionsSupplier
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.*
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPromoRepositoryTest {

    private val promotionsSupplier: PromotionsSupplier = mockk()
    private val tangemApi: TangemTechApi = mockk()
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val repository = createRepository()

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

    private fun createRepository() = DefaultPromoRepository(
        promotionsSupplier = promotionsSupplier,
        tangemApi = tangemApi,
        moshi = moshi,
        campaignsStore = RuntimeSharedStore(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetCampaigns {

        private val coldWallet = MockUserWalletFactory.create()
        private val hotUserWalletId = UserWalletId("0123456789ab")
        private val hotWallet: UserWallet.Hot = mockk {
            every { walletId } returns hotUserWalletId
        }
        private val language = SupportedLanguages.getCurrentSupportedLanguageCode()

        @Test
        fun `GIVEN cold wallet WHEN getCampaigns THEN sends card type with batch id and returns converted`() = runTest {
            // Arrange
            val repository = createRepository()
            stubPromotionsV3(PromotionsV3Response(promotions = listOf(createV3Dto())))

            // Act
            val result = repository.getCampaigns(coldWallet)

            // Assert
            assertThat(result).containsExactly(createExpectedCampaign())
            coVerify(exactly = 1) {
                tangemApi.getPromotionsV3(
                    walletId = coldWallet.walletId.stringValue,
                    walletType = PromotionWalletType.CARD,
                    batchId = coldWallet.scanResponse.card.batchId,
                    languageISOCode = language,
                )
            }
        }

        @Test
        fun `GIVEN hot wallet WHEN getCampaigns THEN sends mobile type without batch id`() = runTest {
            // Arrange
            val repository = createRepository()
            stubPromotionsV3(PromotionsV3Response(promotions = emptyList()))

            // Act
            repository.getCampaigns(hotWallet)

            // Assert
            coVerify(exactly = 1) {
                tangemApi.getPromotionsV3(
                    walletId = hotUserWalletId.stringValue,
                    walletType = PromotionWalletType.MOBILE,
                    batchId = null,
                    languageISOCode = language,
                )
            }
        }

        @Test
        fun `GIVEN campaigns cached WHEN getCampaigns again THEN served from cache`() = runTest {
            // Arrange
            val repository = createRepository()
            stubPromotionsV3(PromotionsV3Response(promotions = listOf(createV3Dto())))
            repository.getCampaigns(coldWallet)

            // Act
            val result = repository.getCampaigns(coldWallet)

            // Assert
            assertThat(result).containsExactly(createExpectedCampaign())
            coVerify(exactly = 1) { tangemApi.getPromotionsV3(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN request in flight WHEN getCampaigns for same wallet THEN one request serves both`() = runTest {
            // Arrange
            val repository = createRepository()
            val responseGate = CompletableDeferred<Unit>()
            coEvery { tangemApi.getPromotionsV3(any(), any(), any(), any()) } coAnswers {
                responseGate.await()
                ApiResponse.Success(PromotionsV3Response(promotions = listOf(createV3Dto())))
            }

            // Act
            val first = async { repository.getCampaigns(coldWallet) }
            val second = async { repository.getCampaigns(coldWallet) }
            runCurrent()
            responseGate.complete(Unit)

            // Assert
            assertThat(first.await()).containsExactly(createExpectedCampaign())
            assertThat(second.await()).containsExactly(createExpectedCampaign())
            coVerify(exactly = 1) { tangemApi.getPromotionsV3(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN campaigns cached WHEN getCampaigns with forceRefresh THEN fetches again`() = runTest {
            // Arrange
            val repository = createRepository()
            stubPromotionsV3(PromotionsV3Response(promotions = listOf(createV3Dto())))
            repository.getCampaigns(coldWallet)
            stubPromotionsV3(PromotionsV3Response(promotions = emptyList()))

            // Act
            val result = repository.getCampaigns(coldWallet, forceRefresh = true)

            // Assert
            assertThat(result).isEmpty()
            coVerify(exactly = 2) { tangemApi.getPromotionsV3(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN api error WHEN getCampaigns THEN throws`() = runTest {
            // Arrange
            val repository = createRepository()
            @Suppress("UNCHECKED_CAST")
            coEvery { tangemApi.getPromotionsV3(any(), any(), any(), any()) } returns ApiResponse.Error(
                ApiResponseError.HttpException(
                    code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
                    message = "server",
                    errorBody = null,
                ),
            ) as ApiResponse<PromotionsV3Response>

            // Act
            val error = runCatching { repository.getCampaigns(coldWallet) }.exceptionOrNull()

            // Assert
            assertThat(error).isNotNull()
        }

        private fun stubPromotionsV3(response: PromotionsV3Response) {
            coEvery { tangemApi.getPromotionsV3(any(), any(), any(), any()) } returns
                ApiResponse.Success(response)
        }

        private fun createV3Dto() = PromotionsV3Response.PromotionDto(
            id = 7,
            name = "example_campaign",
            status = PromotionsV3Response.Status.ACTIVE,
            startAt = "2026-06-01T00:00:00Z",
            endAt = "2026-08-31T23:59:59Z",
            isAlreadyEnrolled = false,
            eligibility = PromotionsV3Response.Eligibility(
                type = PromotionsV3Response.EligibilityType.PUBLIC,
                conditions = null,
            ),
            properties = PromotionsV3Response.Properties(
                isPayoutNeeded = false,
                payoutTokens = null,
                matchMode = PromotionsV3Response.MatchMode.STRICT,
            ),
            content = PromotionsV3Response.Content(
                campaignNameKey = "campaign.example_campaign.title",
                publicDescription = emptyList(),
                publicImage = "https://cdn.tangem.com/c.png",
                learnMoreLink = null,
                tncLink = PromotionsV3Response.Link(textKey = "campaign.example_campaign.terms", url = "https://t.com"),
                actionButtonNameKey = "campaign.example_campaign.action",
            ),
        )

        private fun createExpectedCampaign() = PromoCampaign(
            id = 7,
            name = "example_campaign",
            status = PromoCampaignStatus.Active,
            timeline = PromoTimeline(
                start = Instant.parse("2026-06-01T00:00:00Z"),
                end = Instant.parse("2026-08-31T23:59:59Z"),
            ),
            isAlreadyEnrolled = false,
            eligibility = PromoEligibility.Public,
            payout = PromoPayout(isNeeded = false, tokens = emptyList()),
            matchMode = PromoMatchMode.Strict,
            content = PromoCampaignContent(
                nameKey = "campaign.example_campaign.title",
                description = emptyList(),
                imageUrl = "https://cdn.tangem.com/c.png",
                learnMoreLink = null,
                tncLink = PromoLink(textKey = "campaign.example_campaign.terms", url = "https://t.com"),
                actionButtonNameKey = "campaign.example_campaign.action",
            ),
        )
    }
}