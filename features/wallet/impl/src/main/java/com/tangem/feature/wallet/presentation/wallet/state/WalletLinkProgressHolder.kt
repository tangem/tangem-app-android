package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Wallets being linked to the device. The banner is rebuilt on every balance or quote update, so its progress is
 * read from here rather than kept in the rendered state.
 */
@ModelScoped
internal class WalletLinkProgressHolder @Inject constructor() {

    private val linkingWalletIds = MutableStateFlow(emptySet<UserWalletId>())

    fun isLinking(userWalletId: UserWalletId): Flow<Boolean> {
        return linkingWalletIds.map { userWalletId in it }.distinctUntilChanged()
    }

    /** Returns `false` when this wallet is already being linked. */
    fun tryStart(userWalletId: UserWalletId): Boolean {
        return userWalletId !in linkingWalletIds.getAndUpdate { it + userWalletId }
    }

    fun finish(userWalletId: UserWalletId) {
        linkingWalletIds.update { it - userWalletId }
    }
}