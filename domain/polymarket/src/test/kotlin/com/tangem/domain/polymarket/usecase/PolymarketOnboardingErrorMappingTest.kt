package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.test.core.ProvideTestModels
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
    fun `GIVEN a wallet error WHEN mapped THEN it is wrapped without losing the cause`() {
        // Arrange
        val cause = PolymarketWalletError.RelayerRejected.NonceReused

        // Act
        val actual = cause.toOnboardingError()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Wallet(cause))
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

    private fun provideTestModels() = listOf(
        PolymarketDerivationError.MissingWallet,
        PolymarketDerivationError.UserCancelled,
        PolymarketDerivationError.DerivationUnsupported,
        PolymarketDerivationError.CardError,
        PolymarketDerivationError.Unknown,
    )
}