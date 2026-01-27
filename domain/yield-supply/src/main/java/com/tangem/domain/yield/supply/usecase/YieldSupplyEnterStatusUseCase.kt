package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus

class YieldSupplyEnterStatusUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, YieldSupplyPendingStatus?> {
        return Either.catch {
            val status = yieldSupplyRepository.getTokenProtocolPendingStatus(
                userWalletId,
                cryptoCurrencyStatus.currency,
            )
            val pendingTxHashes = yieldSupplyRepository
                .getPendingTxHashes(userWalletId, cryptoCurrencyStatus.currency)
                .toSet()
            val hasPendingTx = status?.txIds?.any { it in pendingTxHashes } == true

            if (hasPendingTx) {
                status
            } else {
                val isActive = cryptoCurrencyStatus.value.yieldSupplyStatus?.isActive == true
                val isExpired = status != null &&
                    System.currentTimeMillis() - status.createdAt > STATUS_EXPIRATION_MS
                val shouldClearStatus = when {
                    isExpired -> true
                    isActive && status is YieldSupplyPendingStatus.Exit -> false
                    !isActive && status is YieldSupplyPendingStatus.Enter -> false
                    else -> true
                }
                if (shouldClearStatus) {
                    yieldSupplyRepository.saveTokenProtocolPendingStatus(
                        userWalletId,
                        cryptoCurrencyStatus.currency,
                        null,
                    )
                    null
                } else {
                    status
                }
            }
        }
    }

    private companion object {
        const val STATUS_EXPIRATION_MS = 20_000L
    }
}