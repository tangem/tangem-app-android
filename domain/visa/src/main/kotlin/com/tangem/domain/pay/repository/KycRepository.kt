package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.KycStartInfo

interface KycRepository {

    /**
     * Returns KYC data to start or continue the survey
     */
    suspend fun getKycStartInfo(): Either<UniversalError, KycStartInfo>
}