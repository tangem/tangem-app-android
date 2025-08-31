package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.KycStartInfo

interface KycRepository {

    /**
     * Returns fresh KYC data to start the survey. Used only for first time launch
     */
    suspend fun getKycStartInfo(address: String, cardId: String): Either<UniversalError, KycStartInfo>

    /**
     * Returns KYC data to continue the survey. Used when KYC wasn't finished by the user
     */
    suspend fun getKycStartInfo(authHeader: String): Either<UniversalError, KycStartInfo>

    interface Factory {
        fun create(): KycRepository
    }
}