package com.tangem.data.polymarket.quote

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

internal class PolymarketQuoteStoreTest {

    private val time = TestTimeSource()
    private val store = PolymarketQuoteStore(time)

    @Test
    fun `GIVEN partial sides WHEN merged THEN a complete quote becomes live and missing sides survive`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4")))
        val waiting = quote()

        // Act
        store.apply(listOf(update(timestamp = 101, ask = "0.6")))

        // Assert
        assertThat(waiting.status).isEqualTo(PolymarketLiveQuote.Status.WAITING)
        assertThat(quote()).isEqualTo(expected(bid = "0.4", ask = "0.6", timestamp = 101))
    }

    @Test
    fun `GIVEN live prices WHEN an empty snapshot arrives THEN both stale sides disappear`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4", ask = "0.6")))

        // Act
        store.apply(listOf(PolymarketQuoteUpdate("a", 101, side(null), side(null))))

        // Assert
        assertThat(quote()).isEqualTo(expected(bid = null, ask = null, timestamp = 101))
    }

    @Test
    fun `GIVEN a newer ask and trade WHEN older book fields arrive THEN each field keeps its own ordering`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4", ask = "0.6")))
        store.apply(listOf(update(timestamp = 110, ask = "0.7")))
        store.apply(listOf(PolymarketQuoteUpdate("a", 120, lastTradePrice = BigDecimal("0.55"))))

        // Act
        store.apply(listOf(update(timestamp = 105, bid = "0.45", ask = "0.65")))
        store.apply(listOf(PolymarketQuoteUpdate("a", 115, lastTradePrice = BigDecimal("0.5"))))

        // Assert
        assertThat(quote()).isEqualTo(expected("0.45", "0.7", 110).copy(lastTradePrice = BigDecimal("0.55")))
    }

    @Test
    fun `GIVEN silent assets WHEN sixty seconds pass THEN one refresh is followed by REST fallback after grace`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4", ask = "0.6")))
        time += 60.seconds
        val before = store.snapshot(setOf("a"), true)

        // Act
        time += 1.milliseconds
        val refresh = store.snapshot(setOf("a"), true)
        val secondConsumer = store.snapshot(setOf("a"), true)
        time += 5.seconds
        val fallback = store.snapshot(setOf("a"), true)

        // Assert
        assertThat(before.resubscribe).isEmpty()
        assertThat(refresh.resubscribe).containsExactly("a")
        assertThat(secondConsumer.resubscribe).isEmpty()
        assertThat(refresh.quotes["a"]).isEqualTo(expected("0.4", "0.6"))
        assertThat(fallback.quotes["a"]).isEqualTo(expected("0.4", "0.6").copy(status = PolymarketLiveQuote.Status.STALE))
        assertThat(fallback.resubscribe).isEmpty()
    }

    @Test
    fun `GIVEN a refresh request WHEN a snapshot returns THEN the quote recovers and the watchdog restarts`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4", ask = "0.6")))
        time += 61.seconds
        store.snapshot(setOf("a"), true)
        time += 6.seconds

        // Act
        store.apply(listOf(update(timestamp = 101, bid = "0.5", ask = "0.7")))
        val recovered = quote()
        time += 61.seconds
        val nextRefresh = store.snapshot(setOf("a"), true)

        // Assert
        assertThat(recovered).isEqualTo(expected("0.5", "0.7", 101))
        assertThat(nextRefresh.resubscribe).containsExactly("a")
    }

    @Test
    fun `GIVEN unchanged valid quotes WHEN repeated THEN freshness advances without changing emitted values`() {
        // Arrange
        val update = update(bid = "0.4", ask = "0.6")
        store.apply(listOf(update))
        time += 50.seconds

        // Act
        store.apply(listOf(update))
        time += 20.seconds
        val snapshot = store.snapshot(setOf("a"), true)

        // Assert
        assertThat(snapshot.quotes["a"]).isEqualTo(expected("0.4", "0.6"))
        assertThat(snapshot.resubscribe).isEmpty()
    }

    @Test
    fun `GIVEN stale book prices WHEN trades and older deltas arrive THEN they cannot conceal a book stall`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4", ask = "0.6")))
        time += 59.seconds

        // Act
        store.apply(listOf(update(timestamp = 99, ask = "0.7")))
        store.apply(listOf(PolymarketQuoteUpdate("a", 200, lastTradePrice = BigDecimal("0.5"))))
        time += 2.seconds
        val snapshot = store.snapshot(setOf("a"), true)

        // Assert
        assertThat(snapshot.resubscribe).containsExactly("a")
        assertThat(snapshot.quotes["a"]).isEqualTo(expected("0.4", "0.6").copy(lastTradePrice = BigDecimal("0.5")))
    }

    @Test
    fun `GIVEN two assets WHEN only one keeps updating THEN freshness and recovery stay independent`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4", ask = "0.6"), update("b", bid = "0.3", ask = "0.7")))
        time += 50.seconds
        store.apply(listOf(update("b", timestamp = 101, ask = "0.65")))

        // Act
        time += 11.seconds
        val snapshot = store.snapshot(setOf("a", "b"), true)

        // Assert
        assertThat(snapshot.resubscribe).containsExactly("a")
        assertThat(snapshot.quotes["b"]).isEqualTo(expected("0.3", "0.65", 101).copy(assetId = "b"))
    }

    @Test
    fun `GIVEN long silent but observed quotes WHEN another asset is read THEN active fallback state is retained`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4", ask = "0.6")))
        time += 299.seconds
        store.snapshot(setOf("a"), true)
        time += 6.seconds

        // Act
        store.snapshot(setOf("b"), true)
        val retained = quote()
        time += 301.seconds
        store.snapshot(setOf("b"), true)
        val expired = quote()

        // Assert
        assertThat(retained).isEqualTo(expected("0.4", "0.6").copy(status = PolymarketLiveQuote.Status.STALE))
        assertThat(expired).isEqualTo(PolymarketLiveQuote("a"))
    }

    @Test
    fun `GIVEN a disconnected socket WHEN reading prices THEN cached quotes request REST without resubscribe`() {
        // Arrange
        store.apply(listOf(update(bid = "0.4", ask = "0.6")))
        time += 61.seconds

        // Act
        val snapshot = store.snapshot(setOf("a"), false)

        // Assert
        assertThat(snapshot.quotes["a"]).isEqualTo(expected("0.4", "0.6").copy(status = PolymarketLiveQuote.Status.STALE))
        assertThat(snapshot.resubscribe).isEmpty()
    }

    private fun quote() = store.snapshot(setOf("a"), true).quotes.getValue("a")

    private fun side(price: String?) = PolymarketQuoteUpdate.Side(price?.let(::BigDecimal))

    private fun update(assetId: String = "a", timestamp: Long = 100, bid: String? = null, ask: String? = null) =
        PolymarketQuoteUpdate(assetId, timestamp, bid?.let(::side), ask?.let(::side))

    private fun expected(bid: String?, ask: String?, timestamp: Long = 100) = PolymarketLiveQuote(
        assetId = "a", bestBid = bid?.let(::BigDecimal), bestAsk = ask?.let(::BigDecimal), updatedAtMillis = timestamp,
        status = PolymarketLiveQuote.Status.LIVE,
    )
}