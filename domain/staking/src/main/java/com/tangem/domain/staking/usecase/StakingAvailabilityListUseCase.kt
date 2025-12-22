package com.tangem.domain.staking.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Returns staking availability for a list of crypto currencies for a specific user wallet
 *
 * Return map:
 * - key: crypto currency
 * - value: staking availability for the currency
 */
class StakingAvailabilityListUseCase(
    private val stakingRepository: StakingRepository,
) {

    suspend fun invokeSync(
        userWalletId: UserWalletId,
        cryptoCurrencyList: List<CryptoCurrency>,
    ): Map<CryptoCurrency, StakingAvailability> {
        return coroutineScope {
            cryptoCurrencyList.map { cryptoCurrency ->
                async {
                    cryptoCurrency to stakingRepository.getStakingAvailabilitySync(
                        userWalletId = userWalletId,
                        cryptoCurrency = cryptoCurrency,
                    )
                }
            }.awaitAll().toMap()
        }
    }
}