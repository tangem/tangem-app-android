package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.KycStartInfo

interface KycRepository {

    /**
     * Returns KYC data to start or continue the survey
     */
    suspend fun getKycStartInfo(userWalletId: UserWalletId): Either<UniversalError, KycStartInfo>
}