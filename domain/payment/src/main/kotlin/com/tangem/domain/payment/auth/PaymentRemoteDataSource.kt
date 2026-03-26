package com.tangem.domain.payment.auth

import arrow.core.Either
import com.tangem.domain.payment.models.auth.PaymentAuthApiError
import com.tangem.domain.payment.models.auth.PaymentAuthChallenge
import com.tangem.domain.payment.models.auth.PaymentAuthTokens

interface PaymentRemoteDataSource {

    suspend fun getCustomerWalletAuthChallenge(
        customerWalletAddress: String,
        customerWalletId: String,
    ): Either<PaymentAuthApiError, PaymentAuthChallenge>

    suspend fun getTokenWithCustomerWallet(
        sessionId: String,
        signature: String,
        nonce: String,
    ): Either<PaymentAuthApiError, PaymentAuthTokens>
}