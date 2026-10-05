package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.polymarket.PolymarketCredentialsLock
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.InvalidatePolymarketCredentialsUseCase

/**
 * Reads the CLOB's collateral balance and allowance of [addresses]'s deposit wallet.
 *
 * The credentials come from the local store, so a wallet that never finished onboarding fails with
 * [PolymarketAuthError.KeyNotFound] without reaching the network.
 *
 * The read is preceded by a refresh, because the CLOB serves a cached view that it only recomputes when
 * asked: without this, a wallet funded after onboarding would report the balance it had at onboarding — zero
 * — for as long as it exists. The refresh is best-effort, so a failure to recompute falls back to reporting
 * the cached figure rather than turning a stale number into an error — except
 * [PolymarketAuthError.InvalidSignature] from either call, which drops the rejected credentials and is returned.
 */
class GetPolymarketBalanceInteractor(
    private val polymarketRepository: PolymarketRepository,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val invalidateCredentials: InvalidatePolymarketCredentialsUseCase,
    private val credentialsLock: PolymarketCredentialsLock,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
    ): Either<PolymarketAuthError, PolymarketBalanceAllowance> = credentialsLock.withLock(addresses.userWalletId) {
        read(addresses)
    }

    private suspend fun read(addresses: PolymarketAddresses): Either<PolymarketAuthError, PolymarketBalanceAllowance> {
        val credentials = getApiCredentials(userWalletId = addresses.userWalletId)
            ?: return PolymarketAuthError.KeyNotFound.left()

        val refresh = polymarketRepository.syncBalanceAllowance(
            ownerAddress = addresses.ownerAddress,
            credentials = credentials,
        )

        if (refresh.leftOrNull() == PolymarketAuthError.InvalidSignature) {
            invalidateCredentials(userWalletId = addresses.userWalletId)

            return PolymarketAuthError.InvalidSignature.left()
        }

        return polymarketRepository.getBalanceAllowance(
            ownerAddress = addresses.ownerAddress,
            credentials = credentials,
        ).onLeft { error ->
            if (error == PolymarketAuthError.InvalidSignature) {
                invalidateCredentials(userWalletId = addresses.userWalletId)
            }
        }
    }
}