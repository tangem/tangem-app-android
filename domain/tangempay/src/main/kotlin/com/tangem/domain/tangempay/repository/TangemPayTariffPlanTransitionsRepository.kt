package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError

interface TangemPayTariffPlanTransitionsRepository {

    suspend fun getTransitions(
        userWalletId: UserWalletId,
    ): Either<TangemPayApiError, List<TangemPayTariffPlanTransition>>

    suspend fun setPendingTransition(
        userWalletId: UserWalletId,
        pendingTariffPlanId: String,
    ): Either<TangemPayApiError, Unit>

    suspend fun cancelPendingTransition(userWalletId: UserWalletId): Either<TangemPayApiError, Unit>
}