package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.util.PaymentMethodConverter
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.moneyaccount.repository.MoneyAccountRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import javax.inject.Inject

internal class DefaultMoneyAccountRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestPerformer: TangemPayRequestPerformer,
) : MoneyAccountRepository {

    override suspend fun getPaymentMethods(): Either<VisaApiError, List<MoneyAccountPaymentMethod>> {
        return requestPerformer.performWithStaticToken {
            tangemPayApi.getPaymentMethods()
        }.map { response ->
            response.result.orEmpty().mapNotNull(PaymentMethodConverter::convert)
        }
    }
}