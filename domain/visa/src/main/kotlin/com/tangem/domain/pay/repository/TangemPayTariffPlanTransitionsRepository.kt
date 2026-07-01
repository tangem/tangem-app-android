package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError

interface TangemPayTariffPlanTransitionsRepository {

    suspend fun getTransitions(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<TangemPayTariffPlanTransition>>
}