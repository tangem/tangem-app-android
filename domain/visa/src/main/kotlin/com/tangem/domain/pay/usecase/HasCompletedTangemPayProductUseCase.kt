package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.visa.error.VisaApiError

class HasCompletedTangemPayProductUseCase(
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Boolean> = either {
        val summary = getCustomerSummaryUseCase(
            userWalletId = userWalletId,
            cachePolicy = CachePolicy.CACHE_FIRST
        ).bind()

        ensureNotNull(summary) { VisaApiError.UnknownWithoutCode }

        val paymentAccount = summary.paymentAccount
        val moneyAccount = summary.moneyAccount
        paymentAccount.activation.status == CustomerSummary.PaymentAccount.Activation.Status.COMPLETED ||
            moneyAccount.activation.status == CustomerSummary.MoneyAccount.Activation.Status.COMPLETED
    }
}