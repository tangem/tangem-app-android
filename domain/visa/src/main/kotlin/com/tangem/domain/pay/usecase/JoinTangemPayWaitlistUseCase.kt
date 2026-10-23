package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.pay.id
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.TangemPayWaitlistEntry
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayWaitlistRepository
import com.tangem.domain.visa.error.VisaApiError
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class JoinTangemPayWaitlistUseCase(
    private val waitlistRepository: TangemPayWaitlistRepository,
    private val onboardingRepository: OnboardingRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId?,
        products: List<TangemPayProduct>,
        entry: TangemPayWaitlistEntry,
        idempotencyKey: String,
    ): Either<VisaApiError, Unit> {
        val results = coroutineScope {
            products
                .map { product ->
                    async {
                        waitlistRepository.join(
                            userWalletId = userWalletId,
                            product = product,
                            entry = entry,
                            idempotencyKey = productIdempotencyKey(idempotencyKey, product),
                        )
                    }
                }
                .awaitAll()
        }
        return either {
            results.forEach { it.bind() }
        }.onRight {
            if (userWalletId != null) {
                hidePaymentAccount(userWalletId)
            }
        }
    }

    private fun productIdempotencyKey(idempotencyKey: String, product: TangemPayProduct): String {
        return UUID.nameUUIDFromBytes("$idempotencyKey:${product.id}".toByteArray()).toString()
    }

    private suspend fun hidePaymentAccount(userWalletId: UserWalletId) {
        onboardingRepository.disableTangemPay(userWalletId)
            .onRight { paymentAccountStatusFetcher(userWalletId) }
    }
}