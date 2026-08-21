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
        fun `GIVEN event name WHEN metricName THEN snake_case name without prefix`(model: MetricNameModel) {
            // Act
            val actual = OtelNameConverter.metricName(model.event)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            MetricNameModel(event = "Transaction sent", expected = "transaction_sent"),
            MetricNameModel(event = "Sign In Screen Opened", expected = "sign_in_screen_opened"),
            MetricNameModel(event = "Data Error", expected = "data_error"),
            MetricNameModel(event = "Signature Request Failed", expected = "signature_request_failed"),
            MetricNameModel(event = "Wallet Created Successfully", expected = "wallet_created_successfully"),
        )
    }

    data class MetricNameModel(val event: String, val expected: String)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CategoryValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN category WHEN categoryValue THEN snake_case value`(model: CategoryModel) {
            // Act
            val actual = OtelNameConverter.categoryValue(model.category)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            CategoryModel(category = "Basic", expected = "basic"),
            CategoryModel(category = "Token / Withdraw", expected = "token_withdraw"),
            CategoryModel(category = "Markets / Chart", expected = "markets_chart"),
            CategoryModel(category = "Wallet Connect", expected = "wallet_connect"),
        )
    }

    data class CategoryModel(val category: String, val expected: String)

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