package com.tangem.tap.common.analytics.handlers.opentelemetry

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class OtelNameConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MetricName {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN event coordinates WHEN metricName THEN snake_case dot-namespaced name`(model: MetricNameModel) {
            // Act
            val actual = OtelNameConverter.metricName(category = model.category, event = model.event)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            MetricNameModel(category = "Basic", event = "Transaction sent", expected = "app_basic_transaction_sent"),
            MetricNameModel(
                category = "Sign In",
                event = "Sign In Screen Opened",
                expected = "app_sign_in_sign_in_screen_opened",
            ),
            MetricNameModel(
                category = "Token / Withdraw",
                event = "Screen Opened",
                expected = "app_token_withdraw_screen_opened",
            ),
            MetricNameModel(
                category = "Markets / Chart",
                event = "Data Error",
                expected = "app_markets_chart_data_error",
            ),
            MetricNameModel(
                category = "Wallet Connect",
                event = "Signature Request Failed",
                expected = "app_wallet_connect_signature_request_failed",
            ),
        )
    }

    data class MetricNameModel(val category: String, val event: String, val expected: String)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AttributeKey {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN param key WHEN attributeKey THEN snake_case key`(model: AttributeKeyModel) {
            // Act
            val actual = OtelNameConverter.attributeKey(model.param)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AttributeKeyModel(param = "Fee type", expected = "fee_type"),
            AttributeKeyModel(param = "Blockchain", expected = "blockchain"),
            AttributeKeyModel(param = "Sign in type", expected = "sign_in_type"),
        )
    }

    data class AttributeKeyModel(val param: String, val expected: String)
}