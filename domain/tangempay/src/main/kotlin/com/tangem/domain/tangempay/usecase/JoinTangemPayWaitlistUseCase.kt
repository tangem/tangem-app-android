package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.tangempay.id
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.TangemPayWaitlistEntry
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.repository.TangemPayWaitlistRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
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
    ): Either<TangemPayApiError, Unit> {
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