package com.tangem.data.feed.search.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.feed.search.model.ItemDTO
import com.tangem.data.feed.search.model.ItemKind
import com.tangem.data.feed.search.model.MarketTokenDTO
import com.tangem.domain.feed.search.model.RecentFeedSearchItem
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.models.currency.CryptoCurrency
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class RecentFeedSearchItemConverterTest {

    @Test
    fun `GIVEN a market token WHEN converted both ways THEN the snapshot survives the round trip`() {
        // Arrange
        val item = RecentFeedSearchItem.MarketToken(
            token = TokenMarketParams(
                id = CryptoCurrency.RawID("bitcoin"),
                name = "Bitcoin",
                symbol = "BTC",
                tokenQuotes = TokenMarketParams.Quotes(
                    currentPrice = BigDecimal("64000.5"),
                    h24Percent = BigDecimal("1.25"),
                    weekPercent = null,
                    monthPercent = null,
                ),
                imageUrl = "https://bitcoin.png",
            ),
        )

        // Act
        val restored = item.toDTO(timestamp = 1).toDomain()

        // Assert
        assertThat(restored).isEqualTo(item)
    }

    @Test
    fun `GIVEN an unknown kind WHEN converted THEN the entry is dropped instead of failing`() {
        // Arrange — what an older version reads after a newer one stored a variant it never heard of
        val unknown = ItemDTO(kind = "article", id = "42", timestamp = 1)

        // Act
        val restored = unknown.toDomain()

        // Assert
        assertThat(restored).isNull()
    }

    @Test
    fun `GIVEN an unparseable price WHEN converted THEN the entry is dropped instead of reading as zero`() {
        // Arrange
        val broken = ItemDTO(
            kind = ItemKind.MARKET_TOKEN,
            id = "bitcoin",
            timestamp = 1,
            marketToken = MarketTokenDTO(name = "Bitcoin", symbol = "BTC", currentPrice = "not-a-number"),
        )

        // Act
        val restored = broken.toDomain()

        // Assert
        assertThat(restored).isNull()
    }

    @Test
    fun `GIVEN a known kind with no payload WHEN converted THEN the entry is dropped`() {
        // Arrange
        val payloadless = ItemDTO(kind = ItemKind.MARKET_TOKEN, id = "bitcoin", timestamp = 1)

        // Act
        val restored = payloadless.toDomain()

        // Assert
        assertThat(restored).isNull()
    }
}