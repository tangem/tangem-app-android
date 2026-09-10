package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord

/**
 * Survives process death on purpose: a batch signed but not yet acknowledged must be resent, never
 * re-signed, because a new signature over a new nonce is a second withdrawal and both would land.
 */
interface PredictionWithdrawalStore {

    suspend fun save(userWalletId: UserWalletId, record: PredictionWithdrawalRecord)

    suspend fun get(userWalletId: UserWalletId): PredictionWithdrawalRecord?

    suspend fun clear(userWalletId: UserWalletId)
}