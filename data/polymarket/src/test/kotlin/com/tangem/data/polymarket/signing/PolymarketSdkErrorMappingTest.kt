package com.tangem.data.polymarket.signing

import com.google.common.truth.Truth.assertThat
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class PolymarketSdkErrorMappingTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SigningError {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a throwable WHEN mapped to signing error THEN expected error`(model: SigningModel) {
            // Act
            val actual = model.throwable.toPolymarketSigningError()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        fun provideTestModels() = listOf(
            SigningModel(TangemSdkError.UserCancelled(), PolymarketSigningError.UserCancelled),
            SigningModel(TangemSdkError.WalletNotFound(), PolymarketSigningError.MissingWallet),
            SigningModel(TangemSdkError.ExceptionError(IllegalStateException()), PolymarketSigningError.Unknown),
            SigningModel(TangemSdkError.MissingPreflightRead(), PolymarketSigningError.CardError),
            SigningModel(RuntimeException(), PolymarketSigningError.Unknown),
        )
    }

    data class SigningModel(val throwable: Throwable, val expected: PolymarketSigningError)
}