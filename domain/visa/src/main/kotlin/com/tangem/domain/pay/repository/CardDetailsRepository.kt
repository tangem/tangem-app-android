package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.model.TangemPayCardDetails

interface CardDetailsRepository {

    suspend fun getCardBalance(): Either<UniversalError, TangemPayCardBalance>

    suspend fun revealCardDetails(): Either<UniversalError, TangemPayCardDetails>

    suspend fun setPin(pin: String): Either<UniversalError, SetPinResult>

    suspend fun isAddToWalletDone(): Either<UniversalError, Boolean>

    suspend fun setAddToWalletAsDone(): Either<UniversalError, Unit>
}