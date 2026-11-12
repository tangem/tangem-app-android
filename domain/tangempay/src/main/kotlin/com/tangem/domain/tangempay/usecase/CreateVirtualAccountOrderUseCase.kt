package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch

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
        idempotencyKey: String,
    ): Either<TangemPayApiError, Unit> = either {
        onboardingRepository.getVirtualAccountOrderId(userWalletId)
            ?: run {
                val vaOrderId = onboardingRepository.createVirtualAccountOrder(
                    userWalletId = userWalletId,
                    paymentAccountAddress = paymentAccountAddress,
                    idempotencyKey = idempotencyKey,
                ).bind()
                onboardingRepository.storeVirtualAccountOrderFailed(userWalletId = userWalletId, isFailed = false)
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