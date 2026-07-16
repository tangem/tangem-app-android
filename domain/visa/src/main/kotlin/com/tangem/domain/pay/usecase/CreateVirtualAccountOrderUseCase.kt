package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Creates the Virtual Account on-ramp order (VA MVP0, TWI-1638) and persists the returned id as `vaOrderId`.
 *
 * Idempotent: if an order id was already stored for the wallet, it is returned without hitting the network.
 * Otherwise creates the order (`ACCOUNT_ISSUE_VIRTUAL_RAIN`) and stores the returned id.
 *
 * @property onboardingRepository resolves the customer wallet address, creates the order, and persists the id.
 */
class CreateVirtualAccountOrderUseCase(
    private val onboardingRepository: OnboardingRepository,
    private val pollingUseCase: StartTangemPayOrderPollingUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        paymentAccountAddress: String,
    ): Either<VisaApiError, Unit> = either {
        onboardingRepository.getVirtualAccountOrderId(userWalletId)
            ?: run {
                val vaOrderId = onboardingRepository.createVirtualAccountOrder(
                    userWalletId = userWalletId,
                    paymentAccountAddress = paymentAccountAddress,
                    idempotencyKey = UUID.randomUUID().toString(),
                ).bind()
                onboardingRepository.storeVirtualAccountOrderId(userWalletId = userWalletId, vaOrderId = vaOrderId)
                // Optimistically flip the cached on-ramp to Processing so the UI shows "Preparing" immediately
                // (no wait for the poll/refetch to confirm).
                paymentAccountStatusFetcher.markVirtualAccountProcessing(userWalletId)
                appCoroutineScope.launch {
                    pollingUseCase.invoke(
                        order = TangemPayOrderInfo(orderId = vaOrderId, orderStatus = OrderStatus.NEW),
                        userWalletId = userWalletId,
                    )
                }
            }
    }
}