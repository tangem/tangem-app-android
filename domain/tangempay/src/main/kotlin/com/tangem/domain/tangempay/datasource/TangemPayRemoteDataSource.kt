package com.tangem.domain.tangempay.datasource

import arrow.core.Either
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.TangemPayAuthTokens
import com.tangem.domain.tangempay.model.TangemPayAuthChallenge

interface TangemPayRemoteDataSource {

    suspend fun getCustomerWalletAuthChallenge(
        customerWalletAddress: String,
        customerWalletId: String,
    ): Either<TangemPayApiError, TangemPayAuthChallenge>

    suspend fun getTokenWithCustomerWallet(
        sessionId: String,
        signature: String,
        nonce: String,
    ): Either<TangemPayApiError, TangemPayAuthTokens>
}