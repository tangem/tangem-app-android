package com.tangem.domain.pay.repository

import arrow.core.Either

interface CustomerWalletAuthRepository {

    suspend fun generateNewAuthHeader(address: String, cardId: String): Either<Throwable, String>
}