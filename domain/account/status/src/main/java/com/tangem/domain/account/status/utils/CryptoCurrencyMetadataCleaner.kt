package com.tangem.domain.account.status.utils

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.nft.utils.NFTCleaner
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Class to clean up all data related to a specific cryptocurrency in a user's wallet.
 *
 * @property networksCleaner Utility to clean network-related data.
 * @property stakingCleaner Utility to clean staking-related data.
 * @property nftCleaner Utility to clean NFT-related data.
 * @property dispatchers Coroutine dispatchers for managing threading.
 *
 * @see <a href="https://www.notion.so/tangem/2be5d34eb67880008f95cb779dcafac9">Notion</a>
 *
[REDACTED_AUTHOR]
 */
class CryptoCurrencyMetadataCleaner(
    private val networksCleaner: NetworksCleaner,
    private val stakingCleaner: StakingCleaner,
    private val nftCleaner: NFTCleaner,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Cleans up data for a single cryptocurrency in the specified user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currency The cryptocurrency to be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency) {
        invoke(userWalletId = userWalletId, currencies = listOf(currency))
    }

    /**
     * Cleans up data for multiple cryptocurrencies in the specified user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currencies The list of cryptocurrencies to be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        return withContext(dispatchers.default) {
            awaitAll(
                async { networksCleaner(userWalletId = userWalletId, currencies = currencies) },
                async { clearStaking(userWalletId = userWalletId, currencies = currencies) },
                async { clearNFTs(userWalletId = userWalletId, currencies = currencies) },
            )
        }
    }

    private suspend fun clearStaking(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        stakingCleaner(userWalletId = userWalletId, currencies = currencies)
    }

    private suspend fun clearNFTs(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network)

        nftCleaner(userWalletId = userWalletId, networks = networks)
    }
}