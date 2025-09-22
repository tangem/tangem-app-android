package com.tangem.domain.pay.datasource

import arrow.core.Either
import com.tangem.domain.visa.model.VisaAuthTokens

interface TangemPayAuthDataSource {

    suspend fun generateNewAuthTokens(address: String, cardId: String): Either<Throwable, VisaAuthTokens>

    suspend fun refreshAuthTokens(refreshToken: String): Either<Throwable, VisaAuthTokens>
}