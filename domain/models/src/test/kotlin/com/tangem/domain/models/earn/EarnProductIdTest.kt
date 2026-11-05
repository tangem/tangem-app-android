package com.tangem.domain.models.earn

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class EarnProductIdTest {

    private val json = Json

    @Test
    fun `GIVEN product id WHEN encode THEN it is written as a plain json string`() {
        // Arrange
        val productId = EarnProductId("lending")

        // Act
        val actual = json.encodeToString(EarnProductId.serializer(), productId)

        // Assert
        assertThat(actual).isEqualTo("\"lending\"")
    }

    @Test
    fun `GIVEN json string WHEN decode THEN product id is restored`() {
        // Act
        val actual = json.decodeFromString(EarnProductId.serializer(), "\"vault\"")

        // Assert
        assertThat(actual).isEqualTo(EarnProductId("vault"))
    }
}