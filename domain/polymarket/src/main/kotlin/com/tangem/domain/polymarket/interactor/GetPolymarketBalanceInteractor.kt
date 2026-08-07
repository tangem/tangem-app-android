package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase

/**
 * Reads the CLOB's collateral balance and allowance of [addresses]'s deposit wallet.
 *
 * The credentials come from the local store, so a wallet that never finished onboarding fails with
 * [PolymarketAuthError.KeyNotFound] without reaching the network.
 */
class GetPolymarketBalanceInteractor(
    private val polymarketRepository: PolymarketRepository,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
    ): Either<PolymarketAuthError, PolymarketBalanceAllowance> {
        val credentials = getApiCredentials(userWalletId = addresses.userWalletId)
            ?: return PolymarketAuthError.KeyNotFound.left()

        return polymarketRepository.getBalanceAllowance(
            ownerAddress = addresses.ownerAddress,
            credentials = credentials,
        )
    }
}