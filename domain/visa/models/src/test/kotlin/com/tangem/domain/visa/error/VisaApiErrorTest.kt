package com.tangem.domain.visa.error

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VisaApiErrorTest {

    @Test
    fun `GIVEN backend code 140141 WHEN mapped THEN unsupported configuration error`() {
        // Act
        val error = VisaApiError.fromBackendError(backendErrorCode = 140141)

        // Assert
        assertThat(error).isEqualTo(VisaApiError.BankOfframpUnsupportedConfiguration)
    }

    @Test
    fun `GIVEN an unmapped backend code WHEN mapped THEN unknown error with the universal code`() {
        // Act
        val error = VisaApiError.fromBackendError(backendErrorCode = 140199)

        // Assert
        assertThat(error).isEqualTo(VisaApiError.Unknown(errorCode = 104140199))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN api error WHEN isOutcomeUnknown THEN only network, server and session failures are unknown`(case: Case) {
        // Act
        val isOutcomeUnknown = case.error.isOutcomeUnknown()

        // Assert
        assertThat(isOutcomeUnknown).isEqualTo(case.isOutcomeUnknown)
    }

    @Test
    fun `GIVEN session failure WHEN isRetryable THEN it is not retried`() {
        // Act
        val isRetryable = VisaApiError.RefreshTokenExpired.isRetryable()

        // Assert
        assertThat(isRetryable).isFalse()
    }

    @Test
    fun `GIVEN all api errors WHEN listing cases THEN every subtype is covered`() {
        // Act
        val covered = provideTestModels().map { it.error::class }.toSet()

        // Assert
        assertThat(covered).containsExactlyElementsIn(VisaApiError::class.sealedSubclasses)
    }

    internal data class Case(val error: VisaApiError, val isOutcomeUnknown: Boolean) {
        override fun toString(): String = "$error -> unknown=$isOutcomeUnknown"
    }

    private fun provideTestModels() = listOf(
        Case(VisaApiError.ServerUnavailable, isOutcomeUnknown = true),
        Case(VisaApiError.UnknownWithoutCode, isOutcomeUnknown = true),
        Case(VisaApiError.RefreshTokenExpired, isOutcomeUnknown = true),
        Case(VisaApiError.Unknown(errorCode = 104000408), isOutcomeUnknown = false),
        Case(VisaApiError.Unspecified, isOutcomeUnknown = false),
        Case(VisaApiError.NotFound, isOutcomeUnknown = false),
        Case(VisaApiError.OrderNotFound, isOutcomeUnknown = false),
        Case(VisaApiError.CustomerIdUnavailable, isOutcomeUnknown = false),
        Case(VisaApiError.CustomerIsBlocked, isOutcomeUnknown = false),
        Case(VisaApiError.ProductInstanceNotFoundActivationRequired, isOutcomeUnknown = false),
        Case(VisaApiError.ProductInstanceIsBlocked, isOutcomeUnknown = false),
        Case(VisaApiError.ProductInstanceIsNotActivated, isOutcomeUnknown = false),
        Case(VisaApiError.ProductInstanceIsAlreadyActivated, isOutcomeUnknown = false),
        Case(VisaApiError.CardIssueActiveOrderExists, isOutcomeUnknown = false),
        Case(VisaApiError.CardIssueOfferNotAvailable, isOutcomeUnknown = false),
        Case(VisaApiError.CardIssueInsufficientBalance, isOutcomeUnknown = false),
        Case(VisaApiError.CardIssueInvalidShippingAddress, isOutcomeUnknown = false),
        Case(VisaApiError.CardIssueInvalidEmbossName, isOutcomeUnknown = false),
        Case(VisaApiError.CardActivationInvalidCardData, isOutcomeUnknown = false),
        Case(VisaApiError.CardActivationCardNotPhysical, isOutcomeUnknown = false),
        Case(VisaApiError.CardActivationCardAlreadyActive, isOutcomeUnknown = false),
        Case(VisaApiError.CardActivationCardNotReadyForActivation, isOutcomeUnknown = false),
        Case(VisaApiError.CardActivationActiveOrderExists, isOutcomeUnknown = false),
        Case(VisaApiError.CardReissuePlasticInvalidSourceCard, isOutcomeUnknown = false),
        Case(VisaApiError.CardReissuePlasticActiveOrderExists, isOutcomeUnknown = false),
        Case(VisaApiError.CardReissuePlasticInsufficientBalance, isOutcomeUnknown = false),
        Case(VisaApiError.CardReissuePlasticNotAvailable, isOutcomeUnknown = false),
        Case(VisaApiError.CardReissuePlasticInvalidShippingAddress, isOutcomeUnknown = false),
        Case(VisaApiError.CardReissuePlasticInvalidEmbossName, isOutcomeUnknown = false),
        Case(VisaApiError.BankOfframpUnsupportedConfiguration, isOutcomeUnknown = false),
        Case(VisaApiError.WithdrawalDataError, isOutcomeUnknown = false),
        Case(VisaApiError.SignWithdrawError, isOutcomeUnknown = false),
        Case(VisaApiError.WithdrawError, isOutcomeUnknown = false),
        Case(VisaApiError.WaitlistInvalidEmail, isOutcomeUnknown = false),
        Case(VisaApiError.AccountIssueActiveOrderExists, isOutcomeUnknown = false),
    )
}