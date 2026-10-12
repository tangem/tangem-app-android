package com.tangem.features.promobanners.impl.campaigns.analytics

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult
import com.tangem.domain.promo.models.PromoRegistrationResult.PromoRegistrationStatus
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.AlreadyEnrolledScreenOpened
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.EnrollError
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.EnrollSuccessful
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent.PayoutToken
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoRegistrationOutcome
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoEnrollAnalyticsResolverTest {

    private val campaign = createPromoCampaign(name = "whale-swap-cashback", isPayoutNeeded = true)
    private val payoutToken = PayoutToken(symbol = "USDT", blockchain = "Ethereum")
    private val mainWalletId = UserWalletId("0a")
    private val linkedWalletId = UserWalletId("0b")
    private val thirdWalletId = UserWalletId("0c")

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN registration result WHEN resolve THEN event matches the outcome`(model: ResolveModel) {
        // Arrange
        val outcome = model.outcome ?: PromoRegistrationOutcome.resolve(
            mainWalletId = mainWalletId,
            enrollments = emptyList(),
            result = model.result,
        )

        // Act
        val event = PromoEnrollAnalyticsResolver.resolve(
            campaign = campaign,
            payoutToken = payoutToken,
            result = model.result,
            outcome = outcome,
            walletsRequested = model.walletsRequested,
        )

        // Assert
        assertThat(event).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ResolveModel(
            name = "mixed response counts only newly enrolled wallets",
            result = listOf(
                registered(mainWalletId, PromoRegistrationStatus.Enrolled),
                registered(linkedWalletId, PromoRegistrationStatus.AlreadyEnrolled),
                registered(thirdWalletId, PromoRegistrationStatus.Ineligible),
            ).right(),
            outcome = PromoRegistrationOutcome.Enrolled(
                coverage = PromoRegistrationOutcome.Coverage.Partial,
                enrolledWalletIds = listOf(mainWalletId, linkedWalletId),
            ),
            walletsRequested = 3,
            expected = EnrollSuccessful(campaign, payoutToken, walletsEnrolled = 1, walletsTotal = 3),
        ),
        ResolveModel(
            name = "dropped result row still counts as requested",
            result = listOf(registered(mainWalletId, PromoRegistrationStatus.Enrolled)).right(),
            outcome = PromoRegistrationOutcome.Enrolled(
                coverage = PromoRegistrationOutcome.Coverage.Partial,
                enrolledWalletIds = listOf(mainWalletId),
            ),
            walletsRequested = 2,
            expected = EnrollSuccessful(campaign, payoutToken, walletsEnrolled = 1, walletsTotal = 2),
        ),
        ResolveModel(
            name = "nothing newly enrolled is the already-enrolled response screen",
            result = listOf(registered(mainWalletId, PromoRegistrationStatus.AlreadyEnrolled)).right(),
            expected = AlreadyEnrolledScreenOpened(
                campaign = "whale-swap-cashback",
                source = AlreadyEnrolledScreenOpened.Source.EnrollResponse,
            ),
        ),
        ResolveModel(
            name = "ineligible main wallet in a successful response is unavailable without code",
            result = listOf(registered(mainWalletId, PromoRegistrationStatus.Ineligible)).right(),
            expected = EnrollError(campaign, EnrollError.Reason.Unavailable, errorCode = null),
        ),
        ResolveModel(
            name = "404 is unavailable",
            result = PromoRegistrationError.CampaignNotFound.left(),
            expected = EnrollError(campaign, EnrollError.Reason.Unavailable, errorCode = 404),
        ),
        ResolveModel(
            name = "main wallet not eligible is unavailable",
            result = PromoRegistrationError.MainWalletNotEligible.left(),
            expected = EnrollError(campaign, EnrollError.Reason.Unavailable, errorCode = 400),
        ),
        ResolveModel(
            name = "409 is not active yet",
            result = PromoRegistrationError.CampaignNotActive.left(),
            expected = EnrollError(campaign, EnrollError.Reason.NotActiveYet, errorCode = 409),
        ),
        ResolveModel(
            name = "400 is technical",
            result = httpError(Code.BAD_REQUEST),
            expected = EnrollError(campaign, EnrollError.Reason.Technical, errorCode = 400),
        ),
        ResolveModel(
            name = "401 is technical",
            result = httpError(Code.UNAUTHORIZED),
            expected = EnrollError(campaign, EnrollError.Reason.Technical, errorCode = 401),
        ),
        ResolveModel(
            name = "500 is technical",
            result = httpError(Code.INTERNAL_SERVER_ERROR),
            expected = EnrollError(campaign, EnrollError.Reason.Technical, errorCode = 500),
        ),
        ResolveModel(
            name = "network failure is technical without code",
            result = PromoRegistrationError.Generic(ApiResponseError.NetworkException()).left(),
            expected = EnrollError(campaign, EnrollError.Reason.Technical, errorCode = null),
        ),
        ResolveModel(
            name = "timeout is technical without code",
            result = PromoRegistrationError.Generic(ApiResponseError.TimeoutException()).left(),
            expected = EnrollError(campaign, EnrollError.Reason.Technical, errorCode = null),
        ),
    )

    private fun registered(walletId: UserWalletId, status: PromoRegistrationStatus) =
        PromoRegistrationResult(userWalletId = walletId, status = status, payout = null)

    private fun httpError(code: Code) = PromoRegistrationError.Generic(
        ApiResponseError.HttpException(code = code, message = null, errorBody = null),
    ).left()

    /** [outcome] overrides the resolved one where enrollments matter for it; otherwise it is resolved from [result]. */
    internal data class ResolveModel(
        val name: String,
        val result: Either<PromoRegistrationError, List<PromoRegistrationResult>>,
        val expected: PromoCampaignsAnalyticsEvent,
        val outcome: PromoRegistrationOutcome? = null,
        val walletsRequested: Int = 1,
    ) {
        override fun toString(): String = name
    }
}