package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface PredictionPositionsUpdateListener {

    /**
     * @return the revision a reader compares against the one it last loaded at. A reader that was not
     *  collecting when the change happened — a paused screen, for instance — still sees the difference.
     */
    fun currentRevision(userWalletId: UserWalletId): Long

    fun revisionFlow(userWalletId: UserWalletId): Flow<Long>
}