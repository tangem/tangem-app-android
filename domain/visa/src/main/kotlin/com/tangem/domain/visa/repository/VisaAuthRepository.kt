package com.tangem.domain.visa.repository

import com.tangem.domain.visa.model.VisaAuthChallenge
import com.tangem.domain.visa.model.VisaAuthSignedChallenge
import com.tangem.domain.visa.model.VisaAuthTokens

interface VisaAuthRepository {

    suspend fun getCardAuthChallenge(cardId: String, cardPublicKey: String): VisaAuthChallenge.Card

    suspend fun getCustomerWalletAuthChallenge(cardId: String, walletPublicKey: String): VisaAuthChallenge.Wallet

    suspend fun getAccessTokens(signedChallenge: VisaAuthSignedChallenge): VisaAuthTokens
}