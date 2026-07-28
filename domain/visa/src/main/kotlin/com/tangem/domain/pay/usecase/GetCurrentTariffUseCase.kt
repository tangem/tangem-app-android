package com.tangem.domain.pay.usecase

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.tariffPlan
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import kotlinx.coroutines.flow.firstOrNull

class GetCurrentTariffUseCase(
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Pair<StatusSource, TangemPayCustomerTariffPlan>? {
        paymentAccountStatusFetcher.invoke(userWalletId)
        val currentStatus = paymentAccountStatusSupplier.invoke(userWalletId).firstOrNull()
        val statusSource = currentStatus?.value?.source
        val tariffPlan = currentStatus?.value?.tariffPlan
        return if (statusSource != null && tariffPlan != null) {
            statusSource to tariffPlan
        } else {
            null
        }
    }
}