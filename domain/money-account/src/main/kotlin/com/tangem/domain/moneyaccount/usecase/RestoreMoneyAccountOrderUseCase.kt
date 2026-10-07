package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.OnboardingRepository

class RestoreMoneyAccountOrderUseCase(
    private val orderRepository: MoneyAccountOrderRepository,
    private val onboardingRepository: OnboardingRepository,
    private val trackOrder: TrackMoneyAccountOrderUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        // TODO v_rodionov: [REDACTED_TASK_KEY] handle error of order restoring
        val customerInfo = onboardingRepository.getCustomerInfo(userWalletId).getOrNull() ?: return
        if (isFlowOver(customerInfo)) {
            orderRepository.clearOrder(userWalletId)
        } else {
            // TODO v_rodionov: [REDACTED_TASK_KEY] handle error of order restoring
            val order = orderRepository.findActiveOrder(userWalletId).getOrNull() ?: return
            trackOrder(userWalletId = userWalletId, order = TangemPayOrderInfo.fromOrder(order))
        }
    }

    private fun isFlowOver(customerInfo: CustomerInfo): Boolean {
        val instanceStatus = customerInfo.accountProductInstance?.status
        return customerInfo.state == CustomerInfo.State.FORMER ||
            instanceStatus == CustomerInfo.ProductInstance.Status.DEACTIVATED ||
            instanceStatus == CustomerInfo.ProductInstance.Status.ACTIVE
    }
}