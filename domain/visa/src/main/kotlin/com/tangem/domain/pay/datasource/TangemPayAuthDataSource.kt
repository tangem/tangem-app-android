package com.tangem.domain.pay.datasource

import arrow.core.Either

interface TangemPayAuthDataSource {

    suspend fun generateNewAuthHeader(address: String, cardId: String): Either<Throwable, String>
}