package com.tangem.features.jointaccount.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.BlockaidResult
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.BlockaidSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.ErrorSource
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.JointAccountError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a failure cause WHEN error event built THEN params match`(model: ErrorModel) {
        // Act
        val event = JointAccountError(source = ErrorSource.Sign, cause = model.cause)

        // Assert
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    @Test
    fun `GIVEN a counted signature WHEN Operation Signed built THEN params match the spec`() {
        // Act
        val event = JointAccountAnalyticsEvent.OperationSigned(
            signaturesCollected = 2,
            membersCount = 3,
            threshold = 2,
            isThresholdReached = true,
            blockchain = "Polygon",
            token = "POL",
        )

        // Assert
        assertThat(event.id).isEqualTo("[Joint Account] Operation Signed")
        assertThat(event.params).isEqualTo(
            mapOf(
                "Signatures Collected" to "2",
                "Members Count" to "3",
                "Threshold" to "2",
                "Threshold Reached" to "Yes",
                AnalyticsParam.BLOCKCHAIN to "Polygon",
                AnalyticsParam.TOKEN_PARAM to "POL",
            ),
        )
    }

    data class ErrorModel(val cause: Throwable, val expectedParams: Map<String, String>)

    private fun provideTestModels() = listOf(
        ErrorModel(
            cause = httpError(errorBody = """{"code":"NONCE_MISMATCH"}"""),
            expectedParams = params(errorCode = "NONCE_MISMATCH"),
        ),
        ErrorModel(
            cause = httpError(errorBody = """{"message":"no code here"}"""),
            expectedParams = params(errorCode = "400"),
        ),
        ErrorModel(
            cause = httpError(errorBody = "not a json"),
            expectedParams = params(errorCode = "400"),
        ),
        ErrorModel(
            cause = httpError(errorBody = null),
            expectedParams = params(errorCode = "400"),
        ),
        ErrorModel(
            cause = IllegalStateException(HTTP_MESSAGE),
            expectedParams = params(errorCode = null),
        ),
    )

    private fun httpError(errorBody: String?) = ApiResponseError.HttpException(
        code = ApiResponseError.HttpException.Code.BAD_REQUEST,
        message = HTTP_MESSAGE,
        errorBody = errorBody,
    )

    private fun params(errorCode: String?): Map<String, String> = buildMap {
        put(AnalyticsParam.SOURCE, "Sign")
        errorCode?.let { put(AnalyticsParam.ERROR_CODE, it) }
        put(AnalyticsParam.ERROR_MESSAGE, HTTP_MESSAGE)
    }

    private companion object {
        const val HTTP_MESSAGE = "Bad request"
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Blockaid {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a source and a result WHEN Blockaid Check built THEN id and params match the spec`(
            model: CheckModel,
        ) {
            // Act
            val event = JointAccountAnalyticsEvent.BlockaidCheck(
                source = model.source,
                result = model.result,
                blockchain = "Polygon",
            )

            // Assert
            assertThat(event.id).isEqualTo("[Joint Account] Blockaid Check")
            assertThat(event.params).isEqualTo(
                mapOf("Source" to model.expectedSource, "Result" to model.expectedResult, "Blockchain" to "Polygon"),
            )
        }

        @ParameterizedTest
        @EnumSource(BlockaidSource::class)
        fun `GIVEN a source WHEN Notice Risky Address built THEN id and params match the spec`(source: BlockaidSource) {
            // Act
            val event = JointAccountAnalyticsEvent.NoticeRiskyAddress(source = source, blockchain = "Ethereum")

            // Assert
            assertThat(event.id).isEqualTo("[Joint Account] Notice - Risky Address")
            assertThat(event.params).isEqualTo(mapOf("Source" to source.specValue(), "Blockchain" to "Ethereum"))
        }

        @ParameterizedTest
        @EnumSource(BlockaidSource::class)
        fun `GIVEN a source WHEN Button Sign Anyway built THEN id and params match the spec`(source: BlockaidSource) {
            // Act
            val event = JointAccountAnalyticsEvent.ButtonSignAnyway(source = source, blockchain = "Ethereum")

            // Assert
            assertThat(event.id).isEqualTo("[Joint Account] Button - Sign Anyway")
            assertThat(event.params).isEqualTo(mapOf("Source" to source.specValue(), "Blockchain" to "Ethereum"))
        }

        @ParameterizedTest
        @EnumSource(BlockaidSource::class)
        fun `GIVEN a source WHEN Notice Malicious Transaction built THEN id and params match the spec`(
            source: BlockaidSource,
        ) {
            // Act
            val event = JointAccountAnalyticsEvent.NoticeMaliciousTransaction(source = source, blockchain = "Ethereum")

            // Assert
            assertThat(event.id).isEqualTo("[Joint Account] Notice - Malicious Transaction")
            assertThat(event.params).isEqualTo(mapOf("Source" to source.specValue(), "Blockchain" to "Ethereum"))
        }

        private fun BlockaidSource.specValue(): String = when (this) {
            BlockaidSource.CreateOperation -> "Create Operation"
            BlockaidSource.Sign -> "Sign"
        }

        private fun provideTestModels() = listOf(
            CheckModel(BlockaidSource.CreateOperation, BlockaidResult.Benign, "Create Operation", "Benign"),
            CheckModel(BlockaidSource.Sign, BlockaidResult.Warning, "Sign", "Warning"),
            CheckModel(BlockaidSource.Sign, BlockaidResult.Malicious, "Sign", "Malicious"),
            CheckModel(BlockaidSource.CreateOperation, BlockaidResult.Error, "Create Operation", "Error"),
            CheckModel(BlockaidSource.Sign, BlockaidResult.Timeout, "Sign", "Timeout"),
            CheckModel(
                source = BlockaidSource.CreateOperation,
                result = BlockaidResult.UnsupportedNetwork,
                expectedSource = "Create Operation",
                expectedResult = "Unsupported Network",
            ),
        )
    }

    data class CheckModel(
        val source: BlockaidSource,
        val result: BlockaidResult,
        val expectedSource: String,
        val expectedResult: String,
    )
}