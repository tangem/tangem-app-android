package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.account.isDefaultTariff
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError

class CancelTariffTransitionUseCase(
    private val cancelTangemPayOrderUseCase: CancelTangemPayOrderUseCase,
    private val getTariffTransitionUseCase: GetTangemPayTariffPlanTransitionsUseCase,
    private val submitTariffTransitionUseCase: SubmitTariffTransitionUseCase,
    private val getCurrentTariffUseCase: GetCurrentTariffUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, orderId: String): Either<TangemPayApiError, Unit> = either {
        cancelTangemPayOrderUseCase.invoke(userWalletId, orderId).bind()

        val tariff = getCurrentTariffUseCase(userWalletId).getOrNull() ?: return@either
        if (tariff.isDefaultTariff) {
            val transitions = getTariffTransitionUseCase.invoke(userWalletId).bind()
            val basicPlanTransition = transitions.find { it.plan.isBasicTier } ?: return@either
            submitTariffTransitionUseCase.invoke(userWalletId, basicPlanTransition).bind()
        }
    }
}