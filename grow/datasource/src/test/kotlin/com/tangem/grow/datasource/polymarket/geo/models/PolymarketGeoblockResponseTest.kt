package com.tangem.grow.datasource.polymarket.geo.models

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class PolymarketGeoblockResponseTest {

    private val json = NetworkJsonFactory.create()

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN blocked flag with extra keys WHEN decoded THEN flag is read`(isBlocked: Boolean) {
        // Arrange
        val body = """{"blocked":$isBlocked,"country":"DE","region":"BE"}"""

        // Act
        val actual = json.decodeFromString<PolymarketGeoblockResponse>(body)

        // Assert
        assertThat(actual).isEqualTo(PolymarketGeoblockResponse(blocked = isBlocked))
    }

    @Test
    fun `GIVEN blocked flag as a quoted string WHEN decoded THEN it is read leniently`() {
        // Arrange
        val body = """{"blocked":"true"}"""

        // Act
        val actual = json.decodeFromString<PolymarketGeoblockResponse>(body)

        // Assert
        assertThat(actual).isEqualTo(PolymarketGeoblockResponse(blocked = true))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "<html><body>Access denied</body></html>",
            "",
            "{}",
            """{"country":"DE"}""",
            """{"blocked":null}""",
            """{"blocked":tr""",
        ],
    )
    fun `GIVEN body without a blocked flag WHEN decoded THEN serialization fails`(body: String) {
        // Act
        val actual = runCatching { json.decodeFromString<PolymarketGeoblockResponse>(body) }

        // Assert
        assertThat(actual.exceptionOrNull()).isInstanceOf(SerializationException::class.java)
    }
}