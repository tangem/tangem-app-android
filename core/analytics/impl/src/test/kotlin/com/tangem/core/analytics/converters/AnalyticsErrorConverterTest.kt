package com.tangem.core.analytics.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class AnalyticsErrorConverterTest {

    private val converter = AnalyticsErrorConverter()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CanBeHandled {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN error WHEN canBeHandled THEN returns whether it is reported`(model: CanBeHandledModel) {
            // Act
            val actual = converter.canBeHandled(model.error)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CanBeHandledModel(error = TangemSdkError.UserCancelled(), expected = false),
            CanBeHandledModel(error = TangemSdkError.TagLost(), expected = true),
            CanBeHandledModel(error = BlockchainSdkError.CustomError("rpc is down"), expected = true),
            CanBeHandledModel(error = IllegalStateException("boom"), expected = true),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN error WHEN convert THEN returns error params`(model: ConvertModel) {
            // Act
            val actual = converter.convert(model.error)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<ConvertModel> {
            val cardError = TangemSdkError.TagLost()
            val blockchainError = BlockchainSdkError.CustomError("rpc is down")
            val cardSdkParams = mapOf(
                AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.CardSdk.value,
                AnalyticsParam.ERROR_CODE to cardError.code.toString(),
                AnalyticsParam.ERROR_DESCRIPTION to cardError.toString(),
            )

            return listOf(
                ConvertModel(error = cardError, expected = cardSdkParams),
                ConvertModel(
                    error = blockchainError,
                    expected = mapOf(
                        AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.BlockchainSdk.value,
                        AnalyticsParam.ERROR_CODE to blockchainError.code.toString(),
                        AnalyticsParam.ERROR_DESCRIPTION to "CustomError: ${blockchainError.customMessage}",
                    ),
                ),
                ConvertModel(
                    error = BlockchainSdkError.WrappedTangemError(tangemError = cardError),
                    expected = cardSdkParams,
                ),
                ConvertModel(
                    error = BlockchainSdkError.WrappedTangemError(tangemError = TangemSdkError.UserCancelled()),
                    expected = emptyMap(),
                ),
                ConvertModel(
                    error = BlockchainSdkError.WrappedTangemError(tangemError = mockk<TangemError>(relaxed = true)),
                    expected = emptyMap(),
                ),
                ConvertModel(
                    error = IllegalStateException("boom"),
                    expected = appParams(description = "boom"),
                ),
                ConvertModel(
                    error = IllegalStateException(null, IllegalArgumentException("cause")),
                    expected = appParams(description = "cause"),
                ),
                ConvertModel(
                    error = IllegalStateException(),
                    expected = appParams(description = "unknown"),
                ),
            )
        }

        private fun appParams(description: String) = mapOf(
            AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.App.value,
            AnalyticsParam.ERROR_DESCRIPTION to description,
        )
    }

    data class CanBeHandledModel(val error: Throwable, val expected: Boolean)

    data class ConvertModel(val error: Throwable, val expected: Map<String, String>)
}