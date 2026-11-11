package com.tangem.domain.tangempay.error

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayApiErrorTest {

    @Test
    fun `GIVEN backend code 140141 WHEN mapped THEN unsupported configuration error`() {
        // Act
        val error = TangemPayApiError.fromBackendError(backendErrorCode = 140141)

        // Assert
        assertThat(error).isEqualTo(TangemPayApiError.BankOfframpUnsupportedConfiguration)
    }

    @Test
    fun `GIVEN an unmapped backend code WHEN mapped THEN unknown error with the universal code`() {
        // Act
        val error = TangemPayApiError.fromBackendError(backendErrorCode = 140199)

        // Assert
        assertThat(error).isEqualTo(TangemPayApiError.Unknown(errorCode = 104140199))
    }

    @ParameterizedTest
    @ValueSource(ints = [110208, 110209])
    fun `GIVEN legacy product instance code WHEN fromBackendError THEN maps to Unknown with the same code`(
        backendCode: Int,
    ) {
        // Act
        val error = TangemPayApiError.fromBackendError(backendErrorCode = backendCode)

        // Assert
        assertThat(error).isEqualTo(TangemPayApiError.Unknown(errorCode = 104_000_000 + backendCode))
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
        val isRetryable = TangemPayApiError.RefreshTokenExpired.isRetryable()

        // Assert
        assertThat(isRetryable).isFalse()
    }

    @Test
    fun `GIVEN all api errors WHEN listing cases THEN every subtype is covered`() {
        // Act
        val covered = provideTestModels().map { it.error::class }.toSet()

        // Assert
        assertThat(covered).containsExactlyElementsIn(TangemPayApiError::class.sealedSubclasses)
    }

    internal data class Case(val error: TangemPayApiError, val isOutcomeUnknown: Boolean) {
        override fun toString(): String = "$error -> unknown=$isOutcomeUnknown"
    }

    private fun provideTestModels() = listOf(
        Case(TangemPayApiError.ServerUnavailable, isOutcomeUnknown = true),
        Case(TangemPayApiError.UnknownWithoutCode, isOutcomeUnknown = true),
        Case(TangemPayApiError.RefreshTokenExpired, isOutcomeUnknown = true),
        Case(TangemPayApiError.Unknown(errorCode = 104000408), isOutcomeUnknown = false),
        Case(TangemPayApiError.Unspecified, isOutcomeUnknown = false),
        Case(TangemPayApiError.NotFound, isOutcomeUnknown = false),
        Case(TangemPayApiError.OrderNotFound, isOutcomeUnknown = false),
        Case(TangemPayApiError.CustomerIdUnavailable, isOutcomeUnknown = false),
        Case(TangemPayApiError.CustomerIsBlocked, isOutcomeUnknown = false),
        Case(TangemPayApiError.ProductInstanceNotFoundActivationRequired, isOutcomeUnknown = false),
        Case(TangemPayApiError.ProductInstanceIsAlreadyActivated, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardIssueActiveOrderExists, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardIssueOfferNotAvailable, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardIssueInsufficientBalance, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardIssueInvalidShippingAddress, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardIssueInvalidEmbossName, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardActivationInvalidCardData, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardActivationCardNotPhysical, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardActivationCardAlreadyActive, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardActivationCardNotReadyForActivation, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardActivationActiveOrderExists, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardReissuePlasticInvalidSourceCard, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardReissuePlasticActiveOrderExists, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardReissuePlasticInsufficientBalance, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardReissuePlasticNotAvailable, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardReissuePlasticInvalidShippingAddress, isOutcomeUnknown = false),
        Case(TangemPayApiError.CardReissuePlasticInvalidEmbossName, isOutcomeUnknown = false),
        Case(TangemPayApiError.BankOfframpUnsupportedConfiguration, isOutcomeUnknown = false),
        Case(TangemPayApiError.WithdrawalDataError, isOutcomeUnknown = false),
        Case(TangemPayApiError.SignWithdrawError, isOutcomeUnknown = false),
        Case(TangemPayApiError.WithdrawError, isOutcomeUnknown = false),
        Case(TangemPayApiError.WaitlistInvalidEmail, isOutcomeUnknown = false),
        Case(TangemPayApiError.AccountIssueActiveOrderExists, isOutcomeUnknown = false),
    )
}