package com.tangem.domain.visa.datasource

import arrow.core.Either
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.domain.visa.model.VisaAuthChallenge

interface TangemPayRemoteDataSource {

    suspend fun getCustomerWalletAuthChallenge(
        customerWalletAddress: String,
        customerWalletId: String,
    ): Either<VisaApiError, VisaAuthChallenge.Wallet>

    suspend fun getTokenWithCustomerWallet(
        sessionId: String,
        signature: String,
        nonce: String,
    ): Either<VisaApiError, TangemPayAuthTokens>
}