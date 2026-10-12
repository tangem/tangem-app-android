package com.tangem.lib.auth.session.internal

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Base64

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccessTokenIssuedAtTest {

    private val issuedAt = AccessTokenIssuedAt(Moshi.Builder().build())

    @Test
    fun `GIVEN a token carrying iat WHEN of THEN the issuance instant is returned`() {
        // Arrange
        val token = jwt(payload = """{"iat":1700000000,"sub":"device"}""")

        // Act
        val actual = issuedAt.of(token)

        // Assert
        assertThat(actual).isEqualTo(Instant.fromEpochSeconds(1700000000))
    }

    @Test
    fun `GIVEN a token without iat WHEN of THEN nothing is returned`() {
        // Arrange
        val token = jwt(payload = """{"sub":"device"}""")

        // Act
        val actual = issuedAt.of(token)

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["opaque-token", "header.%%%.signature", "header.eyJub3QiOiJqc29uIg.signature"])
    fun `GIVEN an unreadable token WHEN of THEN nothing is returned`(token: String) {
        // Act
        val actual = issuedAt.of(token)

        // Assert
        assertThat(actual).isNull()
    }

    private fun jwt(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()

        return listOf(
            encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray()),
            encoder.encodeToString(payload.toByteArray()),
            "signature",
        ).joinToString(separator = ".")
    }
}