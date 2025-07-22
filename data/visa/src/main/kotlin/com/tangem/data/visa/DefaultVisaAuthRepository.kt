package com.tangem.data.visa

import arrow.core.Either
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.*
import com.tangem.datasource.api.pay.models.response.VisaErrorResponseJsonAdapter
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaAuthChallenge
import com.tangem.domain.visa.model.VisaAuthSession
import com.tangem.domain.visa.model.VisaAuthSignedChallenge
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
internal class DefaultVisaAuthRepository @Inject constructor(
    @NetworkMoshi private val moshi: Moshi,
    private val visaAuthApi: TangemPayApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : VisaAuthRepository {

    private val visaErrorAdapter = VisaErrorResponseJsonAdapter(moshi)

    override suspend fun getCardAuthChallenge(
        cardId: String,
        cardPublicKey: String,
    ): Either<VisaApiError, VisaAuthChallenge.Card> = withContext(dispatchers.io) {
        request {
            visaAuthApi.generateNonceByCardId(
                GenerateNoneByCardIdRequest(
                    cardId = cardId,
                    cardPublicKey = cardPublicKey,
                ),
            ).getOrThrow()
        }.map { response ->
            VisaAuthChallenge.Card(
                challenge = response.result.nonce,
                session = VisaAuthSession(response.result.sessionId),
            )
        }
    }

    override suspend fun getCardWalletAuthChallenge(
        cardId: String,
        cardWalletAddress: String,
    ): Either<VisaApiError, VisaAuthChallenge.Wallet> = withContext(dispatchers.io) {
        request {
            visaAuthApi.generateNonceByCardWallet(
                GenerateNoneByCardWalletRequest(
                    cardWalletAddress = cardWalletAddress,
                    cardId = cardId,
                ),
            ).getOrThrow()
        }.map { response ->
            VisaAuthChallenge.Wallet(
                challenge = response.result.nonce,
                session = VisaAuthSession(response.result.sessionId),
            )
        }
    }

    override suspend fun getAccessTokens(
        signedChallenge: VisaAuthSignedChallenge,
    ): Either<VisaApiError, VisaAuthTokens> = withContext(dispatchers.io) {
        request {
            when (signedChallenge) {
                is VisaAuthSignedChallenge.ByCardPublicKey -> {
                    visaAuthApi.getAccessTokenByCardId(
                        GetAccessTokenByCardIdRequest(
                            sessionId = signedChallenge.challenge.session.sessionId,
                            signature = signedChallenge.signature,
                            salt = signedChallenge.salt,
                        ),
                    ).getOrThrow()
                }
                is VisaAuthSignedChallenge.ByWallet -> {
                    visaAuthApi.getAccessTokenByCardWallet(
                        GetAccessTokenByCardWalletRequest(
                            sessionId = signedChallenge.challenge.session.sessionId,
                            signature = signedChallenge.signature,
                            salt = signedChallenge.salt,
                        ),
                    ).getOrThrow()
                }
            }
        }.map { response ->
            VisaAuthTokens(
                accessToken = response.result.accessToken,
                refreshToken = VisaAuthTokens.RefreshToken(
                    value = response.result.refreshToken,
                    authType = when (signedChallenge) {
                        is VisaAuthSignedChallenge.ByCardPublicKey -> VisaAuthTokens.RefreshToken.Type.CardId
                        is VisaAuthSignedChallenge.ByWallet -> VisaAuthTokens.RefreshToken.Type.CardWallet
                    },
                ),
            )
        }
    }

    override suspend fun refreshAccessTokens(
        refreshToken: VisaAuthTokens.RefreshToken,
    ): Either<VisaApiError, VisaAuthTokens> = withContext(dispatchers.io) {
        request {
            when (refreshToken.authType) {
                VisaAuthTokens.RefreshToken.Type.CardId ->
                    visaAuthApi.refreshCardIdAccessToken(
                        RefreshTokenByCardIdRequest(refreshToken = refreshToken.value),
                    )
                VisaAuthTokens.RefreshToken.Type.CardWallet ->
                    visaAuthApi.refreshCardIdAccessToken(
                        RefreshTokenByCardIdRequest(refreshToken = refreshToken.value),
                    )
            }.getOrThrow()
        }.map { response ->
            VisaAuthTokens(
                accessToken = response.result.accessToken,
                refreshToken = refreshToken.copy(value = response.result.refreshToken),
            )
        }
    }

    override suspend fun exchangeAccessToken(tokens: VisaAuthTokens): Either<VisaApiError, VisaAuthTokens> {
        return request {
            visaAuthApi.exchangeAccessToken(
                ExchangeAccessTokenRequest(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken.value,
                ),
            ).getOrThrow()
        }.map { response ->
            VisaAuthTokens(
                accessToken = response.result.accessToken,
                refreshToken = VisaAuthTokens.RefreshToken(
                    value = response.result.refreshToken,
                    authType = VisaAuthTokens.RefreshToken.Type.CardWallet,
                ),
            )
        }
    }

    private suspend fun <T : Any> request(requestBlock: suspend () -> T): Either<VisaApiError, T> {
        return runCatching {
            Either.Right(requestBlock())
        }.getOrElse { responseError ->
            if (responseError is ApiResponseError.HttpException &&
                responseError.errorBody != null
            ) {
                val errorCode =
                    visaErrorAdapter.fromJson(responseError.errorBody!!)?.error?.code ?: responseError.code.numericCode
                return Either.Left(VisaApiError.fromBackendError(errorCode))
            }

            return Either.Left(VisaApiError.UnknownWithoutCode)
        }
    }
}