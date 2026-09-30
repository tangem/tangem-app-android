package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.moneyaccount.repository.MoneyAccountRepository
import com.tangem.domain.visa.error.VisaApiError

class GetMoneyAccountFeeTableUseCase(
    private val moneyAccountRepository: MoneyAccountRepository,
) {
    private var cachedPaymentMethods: List<MoneyAccountPaymentMethod>? = null

    suspend operator fun invoke(): Either<VisaApiError, List<MoneyAccountPaymentMethod>> {
        return cachedPaymentMethods?.right()
            ?: moneyAccountRepository.getPaymentMethods().onRight { cachedPaymentMethods = it }
    }
}