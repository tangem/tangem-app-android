package com.tangem.lib.auth.session.internal

import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.utils.converter.TwoWayConverter
import kotlinx.datetime.Instant

/**
 * Maps between the [SessionTokens] domain model and the [TokenApiResponse] wire/storage DTO.
 * `convert` produces the on-disk / on-wire shape; `convertBack` parses ISO-8601 timestamps
 * into [kotlinx.datetime.Instant].
 */
internal object SessionTokensConverter : TwoWayConverter<SessionTokens, TokenApiResponse> {

    override fun convert(value: SessionTokens): TokenApiResponse = TokenApiResponse(
        accessToken = value.accessToken,
        accessTokenExpiresAt = value.accessTokenExpiresAt.toString(),
        refreshToken = value.refreshToken,
        refreshTokenExpiresAt = value.refreshTokenExpiresAt?.toString(),
        walletIds = value.walletIds,
    )

    override fun convertBack(value: TokenApiResponse): SessionTokens = SessionTokens(
        accessToken = value.accessToken,
        accessTokenExpiresAt = Instant.parse(value.accessTokenExpiresAt),
        refreshToken = value.refreshToken,
        refreshTokenExpiresAt = value.refreshTokenExpiresAt?.let(Instant::parse),
        walletIds = value.walletIds,
    )
}