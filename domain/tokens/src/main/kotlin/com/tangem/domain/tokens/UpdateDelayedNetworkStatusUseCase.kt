package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.delay

/**
 * Use case responsible for fetching currency status information, including network status
 * and quotes for a given cryptocurrency. It provides methods to fetch currency status either
 * by providing a specific currency ID or fetching the status of the primary currency.
 *
 * @param networksRepository The repository for retrieving network-related data.
 */

class UpdateDelayedNetworkStatusUseCase(
    private val networksRepository: NetworksRepository,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    /**
     * Fetches the status of a specific cryptocurrency for a given user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param network Network of the cryptocurrency.
     * @param refresh Indicates whether to force a refresh of the status data.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        delayMillis: Long = 0L,
        refresh: Boolean = false,
    ): Either<CurrencyStatusError, Unit> {
        delay(delayMillis)
        return either {
            fetchNetworkStatus(userWalletId, network, refresh)
        }
    }

    private suspend fun Raise<CurrencyStatusError>.fetchNetworkStatus(
        userWalletId: UserWalletId,
        network: Network,
        refresh: Boolean,
    ) {
        if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
            singleNetworkStatusFetcher(
                params = SingleNetworkStatusFetcher.Params.Simple(userWalletId = userWalletId, network = network),
            )
                .mapLeft { CurrencyStatusError.DataError(it) }
        } else {
            catch(
                block = { networksRepository.getNetworkStatusesSync(userWalletId, setOf(network), refresh) },
            ) {
                raise(CurrencyStatusError.DataError(it))
            }
        }
    }
}