package com.tangem.features.wallet.utils

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface UserWalletsFetcher {

    val userWallets: Flow<ImmutableList<UserWalletItemUM>>

    interface Factory {
        fun create(
            messageSender: UiMessageSender,
            onlyMultiCurrency: Boolean,
            onWalletClick: (UserWalletId) -> Unit,
        ): UserWalletsFetcher
    }
}