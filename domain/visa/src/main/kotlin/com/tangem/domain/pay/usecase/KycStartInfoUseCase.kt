package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.KycStartInfo

interface KycStartInfoUseCase {

    suspend operator fun invoke(): Either<UniversalError, KycStartInfo>
}