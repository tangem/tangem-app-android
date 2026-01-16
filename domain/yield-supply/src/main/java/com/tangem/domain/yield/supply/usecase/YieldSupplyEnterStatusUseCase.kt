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
                .getPendingTxHashes(userWalletId, cryptoCurrencyStatus)
                .toSet()
            val hasPendingTx = status?.txIds?.any { it in pendingTxHashes } == true

            if (hasPendingTx) {
                status
            } else {
                yieldSupplyRepository.saveTokenProtocolPendingStatus(
                    userWalletId,
                    cryptoCurrencyStatus.currency,
                    null,
                )
                null
            }
        }
    }
}