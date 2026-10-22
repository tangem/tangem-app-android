package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerOrderRequest
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials
import java.util.UUID

class CreateOnboardingOrderUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tariffPlanTransition: TangemPayTariffPlanTransition?,
        depositCredentials: MoneyAccountDepositCredentials?,
    ): Either<VisaApiError, Unit> {
        return customerOrderRepository.createOrder(
            userWalletId = userWalletId,
            request = CustomerOrderRequest.Onboarding(
                tariffPlanTransition = tariffPlanTransition?.let { transition ->
                    CustomerOrderRequest.TariffPlanTransition(
                        targetTariffPlanId = transition.plan.id,
                        transitionType = transition.type,
                    )
                },
                depositCredentials = depositCredentials,
            ),
            idempotencyKey = UUID.randomUUID().toString(),
        ).map { }
    }
}