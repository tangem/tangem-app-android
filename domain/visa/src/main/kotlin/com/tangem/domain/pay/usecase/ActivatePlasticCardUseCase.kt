package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CardActivationOrder
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.model.isValidCardLastDigits
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
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
    ): Either<VisaApiError, Unit> = either {
        ensure(activationOrder.lastFourDigits.isValidCardLastDigits()) { VisaApiError.Unspecified }

        val order = catch(
            block = {
                customerOrderRepository
                    .createCardActivationOrder(
                        userWalletId = userWalletId,
                        order = activationOrder,
                        idempotencyKey = idempotencyKey,
                    )
                    .bind()
            },
            catch = { handleError(it) },
        )

        paymentAccountStatusFetcher.invoke(userWalletId)

        appCoroutineScope.launch {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(order),
                userWalletId = userWalletId,
            )
        }
    }

    private fun Raise<VisaApiError>.handleError(throwable: Throwable): Nothing {
        TangemLogger.e("Error in ActivatePlasticCardUseCase", throwable)
        raise(VisaApiError.Unspecified)
    }
}