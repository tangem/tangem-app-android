package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.CardActivationOrder
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.model.isValidCardLastDigits
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch

class ActivatePlasticCardUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val appCoroutineScope: AppCoroutineScope,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        activationOrder: CardActivationOrder,
        idempotencyKey: String,
    ): Either<TangemPayApiError, Unit> = either {
        ensure(activationOrder.lastFourDigits.isValidCardLastDigits()) { TangemPayApiError.Unspecified }

        val order = catch(
            block = {
                customerOrderRepository
                    .createOrder(
                        userWalletId = userWalletId,
                        request = CustomerOrderRequest.CardActivation(order = activationOrder),
                        idempotencyKey = idempotencyKey,
                    )
                    .bind()
            },
            catch = { handleError(it) },
        )

        paymentAccountStatusFetcher.invoke(userWalletId)
        paymentAccountStatusFetcher.markCardActivating(
            userWalletId = userWalletId,
            productInstanceId = activationOrder.productInstanceId,
        )

        appCoroutineScope.launch {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(order),
                userWalletId = userWalletId,
            )
        }
    }

    private fun Raise<TangemPayApiError>.handleError(throwable: Throwable): Nothing {
        TangemLogger.e("Error in ActivatePlasticCardUseCase", throwable)
        raise(TangemPayApiError.Unspecified)
    }
}