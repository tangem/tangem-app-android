package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.usecase.GetCustomerSummaryUseCase

class GetTangemPayProductsAvailabilityUseCase(
    private val isMoneyAccountPathOccupiedUseCase: IsMoneyAccountPathOccupiedUseCase,
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<TangemPayApiError, Set<TangemPayProduct>> = either {
        val summary = ensureNotNull(getCustomerSummaryUseCase(userWalletId).bind()) {
            TangemPayApiError.UnknownWithoutCode
        }
        TangemPayProduct.entries.filterTo(mutableSetOf()) { product ->
            product.isProductAvailable(summary = summary, userWalletId = userWalletId)
        }
    }

    private suspend fun TangemPayProduct.isProductAvailable(
        summary: CustomerSummary,
        userWalletId: UserWalletId,
    ): Boolean = when (this) {
        TangemPayProduct.BANKING_ACCOUNT -> {
            val moneyAccount = summary.moneyAccount.activation
            moneyAccount.status.canStartActivation && !isMoneyAccountPathOccupiedUseCase(userWalletId)
        }
        TangemPayProduct.PAYMENT_ACCOUNT -> {
            val paymentAccount = summary.paymentAccount.activation
            paymentAccount.status.canStartActivation
        }
    }
}

private val CustomerSummary.MoneyAccount.Activation.Status.canStartActivation: Boolean
    get() = when (this) {
        CustomerSummary.MoneyAccount.Activation.Status.NOT_STARTED,
        CustomerSummary.MoneyAccount.Activation.Status.FAILED,
        -> true
        CustomerSummary.MoneyAccount.Activation.Status.UNAVAILABLE,
        CustomerSummary.MoneyAccount.Activation.Status.PROCESSING,
        CustomerSummary.MoneyAccount.Activation.Status.COMPLETED,
        CustomerSummary.MoneyAccount.Activation.Status.DEACTIVATED,
        CustomerSummary.MoneyAccount.Activation.Status.UNKNOWN,
        -> false
    }

private val CustomerSummary.PaymentAccount.Activation.Status.canStartActivation: Boolean
    get() = when (this) {
        CustomerSummary.PaymentAccount.Activation.Status.NOT_STARTED,
        CustomerSummary.PaymentAccount.Activation.Status.FAILED,
        -> true
        CustomerSummary.PaymentAccount.Activation.Status.UNAVAILABLE,
        CustomerSummary.PaymentAccount.Activation.Status.PROCESSING,
        CustomerSummary.PaymentAccount.Activation.Status.COMPLETED,
        CustomerSummary.PaymentAccount.Activation.Status.UNKNOWN,
        -> false
    }