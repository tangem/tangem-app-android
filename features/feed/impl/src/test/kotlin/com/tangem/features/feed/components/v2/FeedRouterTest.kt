package com.tangem.features.feed.components.v2

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.nav.FeedRoute
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class FeedRouterTest {

    @Test
    fun `GIVEN screen is not on the stack WHEN push THEN route is added on top`() {
        // Arrange
        val stack = listOf(FeedHomeRoute, newsDetails(articleId = 1))
        val pushed = tokenDetails(id = "bitcoin")

        // Act
        val actual = stack.pushOrPopTo(pushed)

        // Assert
        assertThat(actual).containsExactly(FeedHomeRoute, newsDetails(articleId = 1), pushed).inOrder()
    }

    @Test
    fun `GIVEN same article deeper on the stack WHEN push news THEN stack pops back to it`() {
        // Arrange
        val present = newsDetails(articleId = 41543)
        val stack = listOf(FeedHomeRoute, present, tokenDetails(id = "bitcoin"))

        // Act
        val actual = stack.pushOrPopTo(newsDetails(articleId = 41543, source = "News Page"))

        // Assert
        assertThat(actual).containsExactly(FeedHomeRoute, present).inOrder()
    }

    @Test
    fun `GIVEN same token with refreshed quote WHEN push token details THEN stack pops back to it`() {
        // Arrange
        val present = tokenDetails(id = "bitcoin", price = "77762.60")
        val stack = listOf(FeedHomeRoute, present, newsDetails(articleId = 41543))

        // Act
        val actual = stack.pushOrPopTo(tokenDetails(id = "bitcoin", price = "78445.92"))

        // Assert
        assertThat(actual).containsExactly(FeedHomeRoute, present).inOrder()
    }

    @Test
    fun `GIVEN another token on the stack WHEN push token details THEN route is added on top`() {
        // Arrange
        val stack = listOf(FeedHomeRoute, tokenDetails(id = "bitcoin"))
        val pushed = tokenDetails(id = "ethereum")

        // Act
        val actual = stack.pushOrPopTo(pushed)

        // Assert
        assertThat(actual).containsExactly(FeedHomeRoute, tokenDetails(id = "bitcoin"), pushed).inOrder()
    }

    @Test
    fun `GIVEN screen is already active WHEN push THEN stack is unchanged`() {
        // Arrange
        val stack = listOf(FeedHomeRoute, newsDetails(articleId = 41543))

        // Act
        val actual = stack.pushOrPopTo(newsDetails(articleId = 41543, source = "News Page"))

        // Assert
        assertThat(actual).isEqualTo(stack)
    }

    private fun newsDetails(articleId: Int, source: String = "Token") = FeedRoute.NewsDetails(
        articleId = articleId,
        source = source,
    )

    private fun tokenDetails(id: String, price: String = "1") = FeedRoute.MarketsTokenDetails(
        token = TokenMarketParams(
            id = CryptoCurrency.RawID(id),
            name = id,
            symbol = id,
            tokenQuotes = TokenMarketParams.Quotes(
                currentPrice = BigDecimal(price),
                h24Percent = null,
                weekPercent = null,
                monthPercent = null,
            ),
            imageUrl = null,
        ),
        appCurrency = AppCurrency.Default,
        shouldShowPortfolio = true,
    )
}