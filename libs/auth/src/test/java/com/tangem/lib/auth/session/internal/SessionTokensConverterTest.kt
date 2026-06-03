package com.tangem.lib.auth.session.internal

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.lib.auth.session.SessionTokens
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionTokensConverterTest {

    private val domain = SessionTokens(
        accessToken = "acc",
        accessTokenExpiresAt = Instant.parse("2023-11-14T22:13:20Z"),
        refreshToken = "rt",
        refreshTokenExpiresAt = Instant.parse("2023-11-14T23:13:20Z"),
        walletIds = listOf("w1", "w2"),
    )

    private val dto = TokenApiResponse(
        accessToken = "acc",
        accessTokenExpiresAt = "2023-11-14T22:13:20Z",
        refreshToken = "rt",
        refreshTokenExpiresAt = "2023-11-14T23:13:20Z",
        walletIds = listOf("w1", "w2"),
    )

    @Test
    fun `convert maps domain to DTO with ISO-8601 timestamps`() {
        assertThat(SessionTokensConverter.convert(domain)).isEqualTo(dto)
    }

    @Test
    fun `convertBack maps DTO to domain with parsed Instant timestamps`() {
        assertThat(SessionTokensConverter.convertBack(dto)).isEqualTo(domain)
    }

    @Test
    fun `convert round-trip preserves domain value`() {
        val roundTripped = SessionTokensConverter.convertBack(SessionTokensConverter.convert(domain))
        assertThat(roundTripped).isEqualTo(domain)
    }

    @Test
    fun `null refresh token survives both directions`() {
        val orangeTier = domain.copy(refreshToken = null, refreshTokenExpiresAt = null)
        val orangeDto = dto.copy(refreshToken = null, refreshTokenExpiresAt = null)

        assertThat(SessionTokensConverter.convert(orangeTier)).isEqualTo(orangeDto)
        assertThat(SessionTokensConverter.convertBack(orangeDto)).isEqualTo(orangeTier)
    }

    @Test
    fun `empty walletIds list survives both directions`() {
        val noWallets = domain.copy(walletIds = emptyList())
        val noWalletsDto = dto.copy(walletIds = emptyList())

        assertThat(SessionTokensConverter.convert(noWallets)).isEqualTo(noWalletsDto)
        assertThat(SessionTokensConverter.convertBack(noWalletsDto)).isEqualTo(noWallets)
    }
}