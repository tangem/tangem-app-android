package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Announces that a wallet's positions changed on the backend and every reader must re-fetch. */
class PredictionPositionsInvalidator {

    private val revisions = MutableStateFlow(value = emptyMap<UserWalletId, Long>())

    fun invalidate(userWalletId: UserWalletId) {
        revisions.update { it + (userWalletId to currentRevision(userWalletId) + 1) }
    }

    /**
     * @return the revision a reader compares against the one it last loaded at. A reader that was not
     *  collecting when the change happened — a paused screen, for instance — still sees the difference.
     */
    fun currentRevision(userWalletId: UserWalletId): Long = revisions.value[userWalletId] ?: 0L

    fun revisionFlow(userWalletId: UserWalletId): Flow<Long> =
        revisions.map { it[userWalletId] ?: 0L }.distinctUntilChanged()
}