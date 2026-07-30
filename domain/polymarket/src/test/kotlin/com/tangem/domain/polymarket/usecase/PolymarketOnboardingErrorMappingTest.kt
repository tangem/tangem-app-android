package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketOnboardingErrorMappingTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a derivation error WHEN mapped THEN it is wrapped without losing the cause`(
        cause: PolymarketDerivationError,
    ) {
        // Act
        val actual = cause.toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Derivation(cause))
    }

    @Test
    fun `GIVEN a non-network wallet error WHEN mapped THEN it is wrapped without losing the cause`() {
        // Arrange
        val cause = PolymarketWalletError.RelayerRejected.NonceReused

        // Act
        val actual = cause.toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Wallet(cause))
    }

    @Test
    fun `GIVEN a network wallet error WHEN mapped THEN returns Network`() {
        // Act
        val actual = PolymarketWalletError.Network.toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network)
    }

    @Test
    fun `GIVEN no internet connection WHEN mapped THEN returns Network`() {
        // Act
        val actual = DataError.NetworkError.NoInternetConnection.toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network)
    }

    @Test
    fun `GIVEN a non-network data error WHEN mapped THEN returns Unknown`() {
        // Act
        val actual = DataError.UserWalletError.WrongUserWallet(message = "boom").toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Unknown)
    }

    @Test
    fun `GIVEN no connection to the auth service WHEN mapped THEN returns Network`() {
        // Act
        val actual = PolymarketAuthError.Network.toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network)
    }

    @Test
    fun `GIVEN a missing key WHEN mapped THEN it is wrapped without losing the cause`() {
        // Act
        val actual = PolymarketAuthError.KeyNotFound.toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Auth(PolymarketAuthError.KeyNotFound))
    }

    @Test
    fun `GIVEN a rejected signature WHEN mapped THEN it is wrapped without losing the cause`() {
        // Act
        val actual = PolymarketAuthError.InvalidSignature.toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Auth(PolymarketAuthError.InvalidSignature))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SigningErrors {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a signing error WHEN mapped THEN it is wrapped without losing the cause`(
            cause: PolymarketSigningError,
        ) {
            // Act
            val actual = cause.toOnboardingError()

            // Assert
            assertThat(actual).isEqualTo(PolymarketOnboardingError.Signing(cause))
        }

        private fun provideTestModels() = listOf(
            PolymarketSigningError.NotDerived,
            PolymarketSigningError.MissingWallet,
            PolymarketSigningError.UserCancelled,
            PolymarketSigningError.CardError,
            PolymarketSigningError.Unknown,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsRetryable {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an error WHEN classified THEN retryability matches the contract`(model: RetryModel) {
            // Act
            val actual = model.error.isRetryable()

            // Assert
            assertThat(actual).isEqualTo(model.retryable)
        }

        private fun provideTestModels() = listOf(
            RetryModel(PolymarketOnboardingError.AddressMismatch(expected = "0xa", actual = "0xb"), false),
            RetryModel(PolymarketOnboardingError.Wallet(PolymarketWalletError.InvalidRequest), false),
            RetryModel(PolymarketOnboardingError.Wallet(PolymarketWalletError.Unauthorized), false),
            RetryModel(PolymarketOnboardingError.Signing(PolymarketSigningError.NotDerived), false),
            RetryModel(PolymarketOnboardingError.Signing(PolymarketSigningError.MissingWallet), false),
            RetryModel(PolymarketOnboardingError.Derivation(PolymarketDerivationError.MissingWallet), false),
            RetryModel(
                error = PolymarketOnboardingError.Derivation(PolymarketDerivationError.DerivationUnsupported),
                retryable = false,
            ),
            RetryModel(PolymarketOnboardingError.Signing(PolymarketSigningError.UserCancelled), true),
            RetryModel(PolymarketOnboardingError.Signing(PolymarketSigningError.CardError), true),
            RetryModel(PolymarketOnboardingError.Derivation(PolymarketDerivationError.UserCancelled), true),
            RetryModel(PolymarketOnboardingError.Wallet(PolymarketWalletError.WalletNotDeployed), true),
            RetryModel(
                error = PolymarketOnboardingError.Wallet(PolymarketWalletError.RelayerRejected.DeadlineTooSoon),
                retryable = true,
            ),
            RetryModel(PolymarketOnboardingError.Wallet(PolymarketWalletError.RelayerUnavailable), true),
            RetryModel(PolymarketOnboardingError.Auth(PolymarketAuthError.InvalidSignature), true),
            RetryModel(PolymarketOnboardingError.DeploymentFailed, true),
            RetryModel(PolymarketOnboardingError.ApprovalsFailed, true),
            RetryModel(PolymarketOnboardingError.Network, true),
            RetryModel(PolymarketOnboardingError.Unknown, true),
        )
    }

    internal data class RetryModel(val error: PolymarketOnboardingError, val retryable: Boolean)

    private fun provideTestModels() = listOf(
        PolymarketDerivationError.MissingWallet,
        PolymarketDerivationError.UserCancelled,
        PolymarketDerivationError.DerivationUnsupported,
        PolymarketDerivationError.CardError,
        PolymarketDerivationError.Unknown,
    )
}