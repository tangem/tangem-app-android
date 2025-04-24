package com.tangem.data.visa

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.visa.TangemVisaAuthApi
import com.tangem.datasource.api.visa.models.request.*
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
    private val visaAuthApi: TangemVisaAuthApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : VisaAuthRepository {

    override suspend fun getCardAuthChallenge(cardId: String, cardPublicKey: String): VisaAuthChallenge.Card =
        withContext(dispatchers.io) {
            val response = visaAuthApi.generateNonceByCardId(
                GenerateNoneByCardIdRequest(
                    cardId = cardId,
                    cardPublicKey = cardPublicKey,
                ),
            )
            VisaAuthChallenge.Card(
                challenge = response.result.nonce,
                session = VisaAuthSession(response.result.sessionId),
            )
        }

    override suspend fun getCardWalletAuthChallenge(cardWalletAddress: String): VisaAuthChallenge.Wallet =
        withContext(dispatchers.io) {
            val response = visaAuthApi.generateNonceByCardWallet(
                GenerateNoneByCardWalletRequest(
                    cardWalletAddress = cardWalletAddress,
                ),
            )
            VisaAuthChallenge.Wallet(
                challenge = response.result.nonce,
                session = VisaAuthSession(response.result.sessionId),
            )
        }

    override suspend fun getAccessTokens(signedChallenge: VisaAuthSignedChallenge): VisaAuthTokens =
        withContext(dispatchers.io) {
            val response = when (signedChallenge) {
                is VisaAuthSignedChallenge.ByCardPublicKey -> {
                    visaAuthApi.getAccessTokenByCardId(
                        GetAccessTokenByCardIdRequest(
                            sessionId = signedChallenge.challenge.session.sessionId,
                            signature = signedChallenge.signature,
                            salt = signedChallenge.salt,
                        ),
                    )
                }
                is VisaAuthSignedChallenge.ByWallet -> {
                    visaAuthApi.getAccessTokenByCardWallet(
                        GetAccessTokenByCardWalletRequest(
                            sessionId = signedChallenge.challenge.session.sessionId,
                            signature = signedChallenge.signature,
                        ),
                    )
                }
            }
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

    override suspend fun refreshAccessTokens(refreshToken: VisaAuthTokens.RefreshToken): VisaAuthTokens =
        withContext(dispatchers.io) {
            val response = when (refreshToken.authType) {
                VisaAuthTokens.RefreshToken.Type.CardId ->
                    visaAuthApi.refreshCardIdAccessToken(
                        RefreshTokenByCardIdRequest(refreshToken = refreshToken.value),
                    )
                VisaAuthTokens.RefreshToken.Type.CardWallet ->
                    visaAuthApi.refreshCardIdAccessToken(
                        RefreshTokenByCardIdRequest(refreshToken = refreshToken.value),
                    )
            }.getOrThrow()

            VisaAuthTokens(
                accessToken = response.result.accessToken,
                refreshToken = refreshToken.copy(value = response.result.refreshToken),
            )
        }
}