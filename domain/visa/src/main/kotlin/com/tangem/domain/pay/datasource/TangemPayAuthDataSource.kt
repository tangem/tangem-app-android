package com.tangem.domain.pay.datasource

import arrow.core.Either
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.domain.visa.model.TangemPayInitialCredentials

interface TangemPayAuthDataSource {

    suspend fun produceInitialCredentials(cardId: String): Either<Throwable, TangemPayInitialCredentials>

    suspend fun refreshAuthTokens(refreshToken: String): Either<Throwable, TangemPayAuthTokens>

    suspend fun getWithdrawalSignature(cardId: String, hash: String): Either<Throwable, String>
}