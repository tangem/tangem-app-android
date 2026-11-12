package com.tangem.data.tangempay.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.data.tangempay.converter.TangemPayTariffPlanConverter
import com.tangem.spend.datasource.tangempay.TangemPayApi
import com.tangem.spend.datasource.tangempay.models.request.SetPendingTariffPlanTransitionRequest
import com.tangem.spend.datasource.tangempay.models.response.TariffPlanTransitionResponse
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.TangemPayTariffPlanTransitionsRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import javax.inject.Inject

internal class DefaultTariffPlanTransitionsRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : TangemPayTariffPlanTransitionsRepository {

    override suspend fun getTransitions(
        userWalletId: UserWalletId,
    ): Either<TangemPayApiError, List<TangemPayTariffPlanTransition>> = either {
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getTariffPlanTransitions(authHeader = authHeader)
        }.bind()

        response.result.orEmpty().mapNotNull { it.toDomain() }
    }

    override suspend fun setPendingTransition(
        userWalletId: UserWalletId,
        pendingTariffPlanId: String,
    ): Either<TangemPayApiError, Unit> = requestHelper.performRequest(userWalletId) { authHeader ->
        tangemPayApi.setPendingTariffPlanTransition(
            authHeader = authHeader,
            body = SetPendingTariffPlanTransitionRequest(pendingTariffPlanId = pendingTariffPlanId),
        )
    }.map {}

    override suspend fun cancelPendingTransition(userWalletId: UserWalletId): Either<TangemPayApiError, Unit> =
        requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.cancelPendingTariffPlanTransition(authHeader = authHeader)
        }.map {}

    private fun TariffPlanTransitionResponse.toDomain(): TangemPayTariffPlanTransition? {
        val plan = TangemPayTariffPlanConverter.convert(tariffPlan) ?: return null
        return TangemPayTariffPlanTransition(
            type = TangemPayTariffPlanTransition.Type.fromString(type),
            plan = plan,
        )
    }
}