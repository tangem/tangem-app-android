package com.tangem.domain.visa.repository

import arrow.core.Either
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.VisaAuthChallenge
import com.tangem.domain.visa.model.VisaAuthSignedChallenge
import com.tangem.domain.visa.model.VisaAuthTokens

interface VisaAuthRepository {

    suspend fun getCardAuthChallenge(
        cardId: String,
        cardPublicKey: String,
    ): Either<VisaApiError, VisaAuthChallenge.Card>

    suspend fun getCardWalletAuthChallenge(
        cardId: String,
        cardWalletAddress: String,
    ): Either<VisaApiError, VisaAuthChallenge.Wallet>

    suspend fun getCustomerWalletAuthChallenge(
        customerWalletAddress: String,
    ): Either<VisaApiError, VisaAuthChallenge.Wallet>

    suspend fun getTokenWithCustomerWallet(
        sessionId: String,
        signature: String,
        nonce: String,
    ): Either<VisaApiError, String>

    suspend fun getAccessTokens(signedChallenge: VisaAuthSignedChallenge): Either<VisaApiError, VisaAuthTokens>

    suspend fun refreshAccessTokens(refreshToken: VisaAuthTokens.RefreshToken): Either<VisaApiError, VisaAuthTokens>

    suspend fun exchangeAccessToken(tokens: VisaAuthTokens): Either<VisaApiError, VisaAuthTokens>
}