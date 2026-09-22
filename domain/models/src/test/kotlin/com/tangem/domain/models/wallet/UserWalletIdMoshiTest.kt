package com.tangem.domain.models.wallet

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

/**
 * Serializes [UserWalletId] through a Moshi instance without `KotlinJsonAdapterFactory`, so only the
 * generated adapter can satisfy the request. Without `@JsonClass` Moshi would fall back to reflection,
 * which needs kotlin-reflect at runtime and fails on installations where it cannot be loaded.
 *
 * The asserted JSON shape is the one already persisted on user devices by the reflective adapter.
 */
internal class UserWalletIdMoshiTest {

    private val adapter = Moshi.Builder().build().adapter(UserWalletId::class.java)

    @Test
    fun `GIVEN wallet id WHEN toJson THEN persisted shape is kept`() {
        // Arrange
        val userWalletId = UserWalletId(stringValue = "011a")

        // Act
        val actual = adapter.toJson(userWalletId)

        // Assert
        assertThat(actual).isEqualTo("""{"stringValue":"011a"}""")
    }

    @Test
    fun `GIVEN persisted json WHEN fromJson THEN wallet id is restored`() {
        // Arrange
        val json = """{"stringValue":"011a"}"""

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(UserWalletId(stringValue = "011a"))
    }
}