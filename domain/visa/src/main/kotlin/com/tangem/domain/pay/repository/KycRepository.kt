package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.KycStartInfo

interface KycRepository {

    suspend fun getKycStartInfo(address: String, cardId: String): Either<UniversalError, KycStartInfo>

    interface Factory {
        fun create(): KycRepository
    }
}