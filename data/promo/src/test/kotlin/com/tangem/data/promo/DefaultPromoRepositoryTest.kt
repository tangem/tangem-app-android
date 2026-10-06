package com.tangem.data.promo

import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import arrow.core.right
import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.promotion.models.PromotionRegistrationResponse
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsBody
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsResponse
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
import com.tangem.domain.promo.models.PromoEnrollment.PromoPayoutTarget
import com.tangem.domain.promo.models.PromoRegistrationResult.PromoRegisteredPayout
import com.tangem.domain.promo.models.PromoRegistrationResult.PromoRegistrationStatus
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Register {

        private val promotionId = 1
        private val cardWalletId = UserWalletId("aa01")
        private val mobileWalletId = UserWalletId("bb02")
        private val payoutTarget = PromoPayoutTarget("0xUser", "0xToken", "ethereum", "tether")

        @Test
        fun `GIVEN card and mobile enrollments WHEN register THEN body carries batch and payout only where allowed`() =
            runTest {
                // Arrange
                val body = slot<PromotionRegistrationsBody>()
                coEvery { tangemApi.createPromotionRegistrations(promotionId, capture(body)) } returns
                    ApiResponse.Success(PromotionRegistrationsResponse(id = promotionId, results = emptyList()))
                val enrollments = listOf(
                    createEnrollment(cardWalletId, PromoWalletType.Card, batchId = "AF99 0088", isMain = true),
                    createEnrollment(mobileWalletId, PromoWalletType.Mobile, batchId = "AF99 0088", payout = null),
                )

                // Act
                repository.register(promotionId, enrollments)

                // Assert
                val expected = PromotionRegistrationsBody(
                    enrollments = listOf(
                        PromotionRegistrationsBody.EnrollmentDto(
                            walletId = cardWalletId.stringValue,
                            walletType = PromotionWalletType.CARD,
                            batchId = "AF99 0088",
                            userAddress = "0xUser",
                            tokenAddress = "0xToken",
                            networkId = "ethereum",
                            tokenId = "tether",
                            isMain = true,
                        ),
                        PromotionRegistrationsBody.EnrollmentDto(
                            walletId = mobileWalletId.stringValue,
                            walletType = PromotionWalletType.MOBILE,
                            batchId = null,
                            userAddress = null,
                            tokenAddress = null,
                            networkId = null,
                            tokenId = null,
                            isMain = false,
                        ),
                    ),
                )
                assertThat(body.captured).isEqualTo(expected)
            }

        @Test
        fun `GIVEN results with every status WHEN register THEN known ones mapped and unreadable dropped`() = runTest {
            // Arrange
            val payout = PromotionRegistrationsResponse.PayoutDto("0xOld", "0xToken", "ethereum")
            val thirdWalletId = UserWalletId("cc03")
            val fourthWalletId = UserWalletId("dd04")
            coEvery { tangemApi.createPromotionRegistrations(promotionId, any()) } returns ApiResponse.Success(
                PromotionRegistrationsResponse(
                    id = promotionId,
                    results = listOf(
                        createResultDto(cardWalletId.stringValue, PromotionRegistrationsResponse.Status.ENROLLED),
                        createResultDto(
                            walletId = mobileWalletId.stringValue,
                            status = PromotionRegistrationsResponse.Status.ALREADY_ENROLLED,
                            payout = payout,
                        ),
                        createResultDto(thirdWalletId.stringValue, PromotionRegistrationsResponse.Status.INELIGIBLE),
                        createResultDto(fourthWalletId.stringValue, PromotionRegistrationsResponse.Status.UNKNOWN),
                        createResultDto("not_hex", PromotionRegistrationsResponse.Status.ENROLLED),
                    ),
                ),
            )
            val enrollments = listOf(cardWalletId, mobileWalletId, thirdWalletId, fourthWalletId)
                .map { createEnrollment(it, PromoWalletType.Card) }

            // Act
            val result = repository.register(promotionId, enrollments)

            // Assert
            val expected = listOf(
                PromoRegistrationResult(cardWalletId, PromoRegistrationStatus.Enrolled, payout = null),
                PromoRegistrationResult(
                    userWalletId = mobileWalletId,
                    status = PromoRegistrationStatus.AlreadyEnrolled,
                    payout = PromoRegisteredPayout("0xOld", "0xToken", "ethereum"),
                ),
                PromoRegistrationResult(thirdWalletId, PromoRegistrationStatus.Ineligible, payout = null),
            )
            assertThat(result).isEqualTo(expected.right())
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN api error WHEN register THEN mapped to registration error`(model: ErrorModel) = runTest {
            // Arrange
            @Suppress("UNCHECKED_CAST")
            coEvery { tangemApi.createPromotionRegistrations(promotionId, any()) } returns
                ApiResponse.Error(model.cause) as ApiResponse<PromotionRegistrationsResponse>

            // Act
            val result = repository.register(promotionId, listOf(createEnrollment(cardWalletId, PromoWalletType.Card)))

            // Assert
            assertThat(result).isEqualTo(model.expected.left())
        }

        @Test
        fun `GIVEN empty or duplicate enrollments WHEN register THEN Generic error without calling api`() = runTest {
            // Arrange
            val duplicate = createEnrollment(cardWalletId, PromoWalletType.Card)

            // Act
            val emptyResult = repository.register(promotionId, emptyList())
            val duplicateResult = repository.register(promotionId, listOf(duplicate, duplicate.copy(isMain = true)))

            // Assert
            assertThat(emptyResult.leftOrNull()).isInstanceOf(PromoRegistrationError.Generic::class.java)
            assertThat(duplicateResult.leftOrNull()).isInstanceOf(PromoRegistrationError.Generic::class.java)
            coVerify(exactly = 0) { tangemApi.createPromotionRegistrations(any(), any()) }
        }

        private fun provideTestModels(): List<ErrorModel> {
            val campaignNotFound = httpError(Code.NOT_FOUND, """{"code":404,"message":"Campaign not found"}""")
            val tokenNotFound = httpError(Code.NOT_FOUND, """{"code":404,"message":"Token not found"}""")
            val notActive = httpError(Code.CONFLICT, """{"code":409,"message":"Campaign not currently active"}""")
            val mainNotEligible = httpError(Code.BAD_REQUEST, """{"code":"x","message":"Main wallet not eligible"}""")
            val genericBadRequests = listOf(
                "Validation error",
                "Main wallet required",
                "Multiple main wallets",
                "User address mismatch",
            ).map { httpError(Code.BAD_REQUEST, """{"code":400,"message":"$it"}""") }
            val unreadableBadRequest = httpError(Code.BAD_REQUEST, "not json")
            val noBodyNotFound = httpError(Code.NOT_FOUND, errorBody = null)
            val unauthorized = httpError(Code.UNAUTHORIZED, """{"code":401,"message":"Unauthorized"}""")
            val serverError = httpError(Code.INTERNAL_SERVER_ERROR, """{"code":500,"message":"Internal error"}""")
            val networkError = ApiResponseError.NetworkException()

            return listOf(
                ErrorModel(campaignNotFound, PromoRegistrationError.CampaignNotFound),
                ErrorModel(notActive, PromoRegistrationError.CampaignNotActive),
                ErrorModel(mainNotEligible, PromoRegistrationError.MainWalletNotEligible),
            ) + (
                genericBadRequests +
                    listOf(tokenNotFound, unreadableBadRequest, noBodyNotFound, unauthorized, serverError, networkError)
                ).map { ErrorModel(it, PromoRegistrationError.Generic(it)) }
        }

        private fun httpError(code: Code, errorBody: String?) = ApiResponseError.HttpException(
            code = code,
            message = code.name,
            errorBody = errorBody,
        )

        private fun createEnrollment(
            userWalletId: UserWalletId,
            walletType: PromoWalletType,
            batchId: String? = null,
            payout: PromoPayoutTarget? = payoutTarget,
            isMain: Boolean = false,
        ) = PromoEnrollment(
            userWalletId = userWalletId,
            walletType = walletType,
            batchId = batchId,
            payout = payout,
            isMain = isMain,
        )

        private fun createResultDto(
            walletId: String,
            status: PromotionRegistrationsResponse.Status,
            payout: PromotionRegistrationsResponse.PayoutDto? = null,
        ) = PromotionRegistrationsResponse.ResultDto(walletId = walletId, status = status, payout = payout)
    }

    data class ErrorModel(val cause: ApiResponseError, val expected: PromoRegistrationError)
}