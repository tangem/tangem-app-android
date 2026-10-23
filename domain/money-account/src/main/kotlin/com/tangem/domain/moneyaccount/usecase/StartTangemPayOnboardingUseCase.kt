package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.moneyaccount.model.StartTangemPayOnboardingError
import com.tangem.domain.pay.usecase.CreateOnboardingOrderUseCase

class StartTangemPayOnboardingUseCase(
    private val activateMoneyAccountUseCase: ActivateMoneyAccountUseCase,
    private val createOnboardingOrderUseCase: CreateOnboardingOrderUseCase,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tariffPlanTransition: TangemPayTariffPlanTransition?,
        availableProducts: Set<TangemPayProduct>,
    ): Either<StartTangemPayOnboardingError, Unit> = either {
        val depositCredentials = if (TangemPayProduct.BANKING_ACCOUNT in availableProducts) {
            activateMoneyAccountUseCase(userWalletId)
                .mapLeft { error ->
                    when (error) {
                        MoneyAccountActivationError.UserCancelled -> StartTangemPayOnboardingError.UserCancelled
                        MoneyAccountActivationError.Unknown -> StartTangemPayOnboardingError.Failed
                    }
                }
                .bind()
        } else {
            null
        }

        createOnboardingOrderUseCase(
            userWalletId = userWalletId,
            tariffPlanTransition = tariffPlanTransition,
            depositCredentials = depositCredentials,
        )
            .mapLeft { StartTangemPayOnboardingError.Failed }
            .bind()
    }
}