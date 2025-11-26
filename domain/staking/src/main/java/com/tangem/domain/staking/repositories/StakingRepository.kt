package com.tangem.domain.staking.repositories

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import kotlinx.coroutines.flow.Flow

interface StakingRepository {

    fun getStakingAvailability(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Flow<StakingAvailability>

    suspend fun getStakingAvailabilitySync(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): StakingAvailability

    suspend fun isAnyTokenStaked(userWalletId: UserWalletId): Boolean
}