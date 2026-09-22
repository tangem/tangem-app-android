package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketDepositRequisites
import kotlinx.coroutines.flow.Flow

/**
 * Session store of the funding requisites.
 *
 * Runtime only by design: an absent entry and one the bridge has not confirmed in this session are the same
 * state, so there is no way to offer an address nothing just confirmed.
 */
interface PredictionDepositRequisitesStore {

    fun get(userWalletId: UserWalletId): Flow<PolymarketDepositRequisites?>

    suspend fun getSyncOrNull(userWalletId: UserWalletId): PolymarketDepositRequisites?

    suspend fun store(userWalletId: UserWalletId, value: PolymarketDepositRequisites)

    suspend fun clear(userWalletId: UserWalletId)
}