package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.models.wallet.UserWalletId

interface KycRepository {

    suspend fun getKycStartInfo(): Either<UniversalError, KycStartInfo>

    interface Factory {
        fun create(userWalletId: UserWalletId): KycRepository
    }
}