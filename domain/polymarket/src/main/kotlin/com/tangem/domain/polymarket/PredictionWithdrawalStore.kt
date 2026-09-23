package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord

interface PredictionWithdrawalStore {

    suspend fun save(userWalletId: UserWalletId, record: PredictionWithdrawalRecord)

    suspend fun get(userWalletId: UserWalletId): PredictionWithdrawalRecord?

    suspend fun clear(userWalletId: UserWalletId)
}