package com.tangem.domain.tangempay.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IssueAdditionalCardErrorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN issue error WHEN isOutcomeUnknown THEN only a wrapped unknown outcome is unknown`(case: Case) {
        // Act
        val isOutcomeUnknown = case.error.isOutcomeUnknown()

        // Assert
        assertThat(isOutcomeUnknown).isEqualTo(case.isOutcomeUnknown)
    }

    internal data class Case(val error: IssueAdditionalCardError, val isOutcomeUnknown: Boolean) {
        override fun toString(): String = "$error -> unknown=$isOutcomeUnknown"
    }

    private fun provideTestModels() = listOf(
        Case(IssueAdditionalCardError.Other(TangemPayApiError.ServerUnavailable), isOutcomeUnknown = true),
        Case(IssueAdditionalCardError.Other(TangemPayApiError.UnknownWithoutCode), isOutcomeUnknown = true),
        Case(IssueAdditionalCardError.Other(TangemPayApiError.RefreshTokenExpired), isOutcomeUnknown = true),
        Case(IssueAdditionalCardError.Other(TangemPayApiError.Unspecified), isOutcomeUnknown = false),
        Case(IssueAdditionalCardError.Other(TangemPayApiError.CardIssueInsufficientBalance), isOutcomeUnknown = false),
        Case(IssueAdditionalCardError.OfferNotAvailable, isOutcomeUnknown = false),
        Case(IssueAdditionalCardError.NoCampaignOffer, isOutcomeUnknown = false),
    )
}