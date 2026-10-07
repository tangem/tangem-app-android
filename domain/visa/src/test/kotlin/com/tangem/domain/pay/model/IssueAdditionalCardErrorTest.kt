package com.tangem.domain.pay.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.visa.error.VisaApiError
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
        Case(IssueAdditionalCardError.Other(VisaApiError.ServerUnavailable), isOutcomeUnknown = true),
        Case(IssueAdditionalCardError.Other(VisaApiError.UnknownWithoutCode), isOutcomeUnknown = true),
        Case(IssueAdditionalCardError.Other(VisaApiError.RefreshTokenExpired), isOutcomeUnknown = true),
        Case(IssueAdditionalCardError.Other(VisaApiError.Unspecified), isOutcomeUnknown = false),
        Case(IssueAdditionalCardError.Other(VisaApiError.CardIssueInsufficientBalance), isOutcomeUnknown = false),
        Case(IssueAdditionalCardError.OfferNotAvailable, isOutcomeUnknown = false),
        Case(IssueAdditionalCardError.NoCampaignOffer, isOutcomeUnknown = false),
    )
}