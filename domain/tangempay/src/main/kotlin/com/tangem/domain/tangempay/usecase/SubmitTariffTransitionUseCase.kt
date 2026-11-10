package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError

class SubmitTariffTransitionUseCase(
    private val createTransitionOrder: CreateTariffPlanTransitionOrderUseCase,
    private val setPendingTransition: SetTariffPlanPendingTransitionUseCase,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        transition: TangemPayTariffPlanTransition,
    ): Either<TangemPayApiError, Unit> {
        return when (transition.type) {
            TangemPayTariffPlanTransition.Type.ACTIVATION,
            TangemPayTariffPlanTransition.Type.UPGRADE,
            -> {
                createTransitionOrder(
                    userWalletId = userWalletId,
                    targetTariffPlanId = transition.plan.id,
                    transitionType = transition.type,
                )
            }
            TangemPayTariffPlanTransition.Type.DOWNGRADE -> {
                setPendingTransition(
                    userWalletId = userWalletId,
                    pendingTariffPlanId = transition.plan.id,
                )
            }
            TangemPayTariffPlanTransition.Type.SYSTEM_DOWNGRADE,
            TangemPayTariffPlanTransition.Type.UNKNOWN,
            -> TangemPayApiError.Unspecified.left()
        }
    }
}