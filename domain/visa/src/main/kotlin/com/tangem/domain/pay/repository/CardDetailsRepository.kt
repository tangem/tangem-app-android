package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails

interface CardDetailsRepository {

    suspend fun getCardBalance(): Either<UniversalError, TangemPayCardBalance>

    suspend fun revealCardDetails(): Either<UniversalError, TangemPayCardDetails>
}