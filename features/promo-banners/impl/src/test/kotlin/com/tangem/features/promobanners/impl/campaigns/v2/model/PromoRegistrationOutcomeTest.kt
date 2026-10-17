package com.tangem.features.promobanners.impl.campaigns.v2.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult
import com.tangem.domain.promo.models.PromoRegistrationResult.PromoRegistrationStatus
import com.tangem.domain.promo.models.PromoWalletType
import com.tangem.features.promobanners.impl.campaigns.v2.model.PromoRegistrationOutcome.Coverage
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoRegistrationOutcomeTest {

    private val mainId = UserWalletId("0a")
    private val otherId = UserWalletId("0b")
    private val payout = PromoRegistrationResult.PromoRegisteredPayout(
        userAddress = "0xUser",
        tokenAddress = "0xToken",
        networkId = "ethereum",
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN registration result WHEN resolve THEN matching outcome`(model: ResolveModel) {
        // Act
        val actual = PromoRegistrationOutcome.resolve(mainId, model.enrollments, model.result)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class ResolveModel(
        val name: String,
        val enrollments: List<PromoEnrollment>,
        val result: Either<PromoRegistrationError, List<PromoRegistrationResult>>,
        val expected: PromoRegistrationOutcome,
    ) {
        override fun toString() = name
    }

    private fun provideTestModels() = listOf(
        ResolveModel(
            name = "single wallet enrolled",
            enrollments = enrollments(mainId),
            result = listOf(result(mainId, PromoRegistrationStatus.Enrolled)).right(),
            expected = PromoRegistrationOutcome.Enrolled(Coverage.Single, listOf(mainId)),
        ),
        ResolveModel(
            name = "every linked wallet in campaign",
            enrollments = enrollments(mainId, otherId),
            result = listOf(
                result(mainId, PromoRegistrationStatus.Enrolled),
                result(otherId, PromoRegistrationStatus.AlreadyEnrolled),
            ).right(),
            expected = PromoRegistrationOutcome.Enrolled(Coverage.All, listOf(mainId, otherId)),
        ),
        ResolveModel(
            name = "a linked wallet ineligible",
            enrollments = enrollments(mainId, otherId),
            result = listOf(
                result(mainId, PromoRegistrationStatus.Enrolled),
                result(otherId, PromoRegistrationStatus.Ineligible),
            ).right(),
            expected = PromoRegistrationOutcome.Enrolled(Coverage.Partial, listOf(mainId)),
        ),
        ResolveModel(
            name = "a linked wallet missing from results",
            enrollments = enrollments(mainId, otherId),
            result = listOf(result(mainId, PromoRegistrationStatus.Enrolled)).right(),
            expected = PromoRegistrationOutcome.Enrolled(Coverage.Partial, listOf(mainId)),
        ),
        ResolveModel(
            name = "main already enrolled with payout",
            enrollments = enrollments(mainId),
            result = listOf(result(mainId, PromoRegistrationStatus.AlreadyEnrolled, payout)).right(),
            expected = PromoRegistrationOutcome.AlreadyEnrolled(payout),
        ),
        ResolveModel(
            name = "main already enrolled without payout",
            enrollments = enrollments(mainId),
            result = listOf(result(mainId, PromoRegistrationStatus.AlreadyEnrolled)).right(),
            expected = PromoRegistrationOutcome.AlreadyEnrolled(payout = null),
        ),
        ResolveModel(
            name = "main already enrolled and a linked wallet newly enrolled",
            enrollments = enrollments(mainId, otherId),
            result = listOf(
                result(mainId, PromoRegistrationStatus.AlreadyEnrolled),
                result(otherId, PromoRegistrationStatus.Enrolled),
            ).right(),
            expected = PromoRegistrationOutcome.Enrolled(Coverage.All, listOf(mainId, otherId)),
        ),
        ResolveModel(
            name = "main ineligible",
            enrollments = enrollments(mainId, otherId),
            result = listOf(
                result(mainId, PromoRegistrationStatus.Ineligible),
                result(otherId, PromoRegistrationStatus.Enrolled),
            ).right(),
            expected = PromoRegistrationOutcome.Unavailable,
        ),
        ResolveModel(
            name = "main missing from results",
            enrollments = enrollments(mainId),
            result = emptyList<PromoRegistrationResult>().right(),
            expected = PromoRegistrationOutcome.Unavailable,
        ),
        ResolveModel(
            name = "campaign not found",
            enrollments = enrollments(mainId),
            result = PromoRegistrationError.CampaignNotFound.left(),
            expected = PromoRegistrationOutcome.Unavailable,
        ),
        ResolveModel(
            name = "main wallet not eligible",
            enrollments = enrollments(mainId),
            result = PromoRegistrationError.MainWalletNotEligible.left(),
            expected = PromoRegistrationOutcome.Unavailable,
        ),
        ResolveModel(
            name = "campaign not active",
            enrollments = enrollments(mainId),
            result = PromoRegistrationError.CampaignNotActive.left(),
            expected = PromoRegistrationOutcome.NotStarted,
        ),
        ResolveModel(
            name = "generic error",
            enrollments = enrollments(mainId),
            result = PromoRegistrationError.Generic(IllegalStateException()).left(),
            expected = PromoRegistrationOutcome.Failed,
        ),
    )

    private fun enrollments(vararg walletIds: UserWalletId) = walletIds.map { walletId ->
        PromoEnrollment(
            userWalletId = walletId,
            walletType = PromoWalletType.Mobile,
            batchId = null,
            payout = null,
            isMain = walletId == mainId,
        )
    }

    private fun result(
        walletId: UserWalletId,
        status: PromoRegistrationStatus,
        payout: PromoRegistrationResult.PromoRegisteredPayout? = null,
    ) = PromoRegistrationResult(userWalletId = walletId, status = status, payout = payout)
}