package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.MoneyAccountDepositCredentials
import java.util.UUID

class CreateOnboardingOrderUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tariffPlanTransition: TangemPayTariffPlanTransition?,
        depositCredentials: MoneyAccountDepositCredentials?,
    ): Either<TangemPayApiError, Unit> {
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