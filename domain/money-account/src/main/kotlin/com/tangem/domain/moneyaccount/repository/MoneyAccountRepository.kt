package com.tangem.domain.moneyaccount.repository

import arrow.core.Either
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.visa.error.VisaApiError

interface MoneyAccountRepository {

    suspend fun getPaymentMethods(): Either<VisaApiError, List<MoneyAccountPaymentMethod>>
}