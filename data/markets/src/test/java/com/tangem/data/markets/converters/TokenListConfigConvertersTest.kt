package com.tangem.data.markets.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/** The request params are a backend contract: a renamed value silently changes what the list returns. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenListConfigConvertersTest {

    @ParameterizedTest
    @ProvideTestModels
    fun order(model: OrderModel) {
        // Act
        val actual = model.order.toRequestParam()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN every order WHEN mapped THEN each one has a distinct request param`() {
        // Act
        val params = TokenMarketListConfig.Order.entries.map { it.toRequestParam() }

        // Assert
        assertThat(params).containsNoDuplicates()
        assertThat(params).hasSize(TokenMarketListConfig.Order.entries.size)
    }

    internal data class OrderModel(val order: TokenMarketListConfig.Order, val expected: String)

    private fun provideTestModels() = listOf(
        OrderModel(order = TokenMarketListConfig.Order.ByRating, expected = "rating"),
        OrderModel(order = TokenMarketListConfig.Order.Trending, expected = "trending"),
        OrderModel(order = TokenMarketListConfig.Order.Buyers, expected = "buyers"),
        OrderModel(order = TokenMarketListConfig.Order.TopGainers, expected = "gainers"),
        OrderModel(order = TokenMarketListConfig.Order.TopLosers, expected = "losers"),
        OrderModel(order = TokenMarketListConfig.Order.ByCategory, expected = "category"),
    )
}