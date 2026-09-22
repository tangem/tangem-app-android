package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class DefaultPredictionPositionsUpdater : PredictionPositionsUpdateEmitter, PredictionPositionsUpdateListener {

    private val revisions = MutableStateFlow(value = emptyMap<UserWalletId, Long>())

    override fun triggerUpdate(userWalletId: UserWalletId) {
        revisions.update { it + (userWalletId to currentRevision(userWalletId) + 1) }
    }

    override fun currentRevision(userWalletId: UserWalletId): Long = revisions.value[userWalletId] ?: 0L

    override fun revisionFlow(userWalletId: UserWalletId): Flow<Long> =
        revisions.map { it[userWalletId] ?: 0L }.distinctUntilChanged()
}