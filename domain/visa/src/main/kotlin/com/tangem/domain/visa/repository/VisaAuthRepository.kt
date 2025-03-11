package com.tangem.domain.visa.repository

import com.tangem.domain.visa.model.VisaAuthChallenge
import com.tangem.domain.visa.model.VisaAuthSignedChallenge
import com.tangem.domain.visa.model.VisaAuthTokens

interface VisaAuthRepository {

    suspend fun getCardAuthChallenge(cardId: String, cardPublicKey: String): VisaAuthChallenge.Card

    suspend fun getCardWalletAuthChallenge(cardWalletAddress: String): VisaAuthChallenge.Wallet

    suspend fun getAccessTokens(signedChallenge: VisaAuthSignedChallenge): VisaAuthTokens

    suspend fun refreshAccessTokens(refreshToken: VisaAuthTokens.RefreshToken): VisaAuthTokens
}