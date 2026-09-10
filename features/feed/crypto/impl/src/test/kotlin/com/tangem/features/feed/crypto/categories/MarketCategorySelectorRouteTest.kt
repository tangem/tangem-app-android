package com.tangem.features.feed.crypto.categories

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/**
 * The route carried a live model reference behind an interface once. It compiled, because kotlinx makes
 * an interface-typed property implicitly polymorphic and only fails when something actually serializes
 * it — which the feed stack, built with `serializer = null`, never does.
 *
 * This test is the compile-time guarantee that was missing: it fails the moment the route grows a
 * property that cannot be serialized.
 */
internal class MarketCategorySelectorRouteTest {

    @Test
    fun `GIVEN the selector route WHEN serialized THEN it round-trips`() {
        // Act
        val encoded = Json.encodeToString(MarketCategorySelectorRoute.serializer(), MarketCategorySelectorRoute)
        val decoded = Json.decodeFromString(MarketCategorySelectorRoute.serializer(), encoded)

        // Assert — the applied filter lives in MarketCategoryFilterController, so the route holds nothing
        assertThat(encoded).isEqualTo("{}")
        assertThat(decoded).isEqualTo(MarketCategorySelectorRoute)
    }
}