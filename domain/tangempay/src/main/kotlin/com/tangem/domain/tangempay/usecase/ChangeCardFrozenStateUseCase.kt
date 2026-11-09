package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.order.OrderLaunch
import com.tangem.domain.tangempay.usecase.order.StartOrderUseCase
import com.tangem.domain.tangempay.order.isCompleted
import com.tangem.domain.tangempay.order.awaitTerminal
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.async

class ChangeCardFrozenStateUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val startOrderUseCase: StartOrderUseCase,
    @RemoveWithToggle(toggleName = "AND_15342_ORDER_FRAMEWORK_ENABLED")
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cardId: String,
        isFreezing: Boolean,
    ): Either<UniversalError, Unit> {
        return if (tangemPayFeatureToggles.isOrderFrameworkEnabled) {
            changeViaOrderFramework(userWalletId = userWalletId, cardId = cardId, isFreezing = isFreezing)
        } else {
            changeViaLegacyPolling(userWalletId = userWalletId, cardId = cardId, isFreezing = isFreezing)
        }
    }

    private suspend fun changeViaOrderFramework(
        userWalletId: UserWalletId,
        cardId: String,
        isFreezing: Boolean,
    ): Either<UniversalError, Unit> = either {
        val orderInfo = freezeCard(userWalletId = userWalletId, cardId = cardId, isFreezing = isFreezing)
        val runningOrder = startOrderUseCase(
            userWalletId = userWalletId,
            launch = OrderLaunch.Id(orderId = orderInfo.orderId),
        ).bind()

        if (!runningOrder.awaitTerminal().isCompleted) {
            raise(TangemPayApiError.Unspecified)
        }
    }

    @RemoveWithToggle(toggleName = "AND_15342_ORDER_FRAMEWORK_ENABLED")
    private suspend fun changeViaLegacyPolling(
        userWalletId: UserWalletId,
        cardId: String,
        isFreezing: Boolean,
    ): Either<UniversalError, Unit> {
        val successState = if (isFreezing) TangemPayCardFrozenState.Frozen else TangemPayCardFrozenState.Unfrozen
        val failState = if (isFreezing) TangemPayCardFrozenState.Unfrozen else TangemPayCardFrozenState.Frozen
        return either {
            cardDetailsRepository.setCardFrozenState(cardId, TangemPayCardFrozenState.Pending)

            val orderInfo = freezeCard(userWalletId = userWalletId, cardId = cardId, isFreezing = isFreezing)

            val isCompleted = appCoroutineScope.async {
                val isCompleted = startTangemPayOrderPollingUseCase(orderInfo, userWalletId)
                cardDetailsRepository.setCardFrozenState(cardId, if (isCompleted) successState else failState)
                isCompleted
            }.await()

            if (!isCompleted) raise(TangemPayApiError.Unspecified)
        }.onLeft {
            cardDetailsRepository.setCardFrozenState(cardId, failState)
        }
    }

    private suspend fun Raise<UniversalError>.freezeCard(
        userWalletId: UserWalletId,
        cardId: String,
        isFreezing: Boolean,
    ): TangemPayOrderInfo {
        return if (isFreezing) {
            cardDetailsRepository.freezeCard(userWalletId, cardId).bind()
        } else {
            cardDetailsRepository.unfreezeCard(userWalletId, cardId).bind()
        }
    }
}