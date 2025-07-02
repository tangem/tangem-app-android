package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.domain.models.network.Network
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.delay

/**
 * Use case responsible for fetching currency status information, including network status
 * and quotes for a given cryptocurrency. It provides methods to fetch currency status either
 * by providing a specific currency ID or fetching the status of the primary currency.
 */
class UpdateDelayedNetworkStatusUseCase(
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
) {

    /**
     * Fetches the status of a specific cryptocurrency for a given user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param network Network of the cryptocurrency.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        delayMillis: Long = 0L,
    ): Either<CurrencyStatusError, Unit> {
        delay(delayMillis)
        return either {
            fetchNetworkStatus(userWalletId, network)
        }
    }

    private suspend fun Raise<CurrencyStatusError>.fetchNetworkStatus(userWalletId: UserWalletId, network: Network) {
        singleNetworkStatusFetcher(
            params = SingleNetworkStatusFetcher.Params(userWalletId = userWalletId, network = network),
        )
            .mapLeft(CurrencyStatusError::DataError)
            .bind()
    }
}