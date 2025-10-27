package com.tangem.domain.pay.datasource

import arrow.core.Either
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.domain.visa.model.VisaAuthTokens

interface TangemPayAuthDataSource {

    suspend fun produceInitialCredentials(cardId: String): Either<Throwable, TangemPayInitialCredentials>

    suspend fun refreshAuthTokens(refreshToken: String): Either<Throwable, VisaAuthTokens>
}