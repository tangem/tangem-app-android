package com.tangem.features.walletconnect.connections.entity

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.ImmutableList

internal data class WcAppInfoWalletUM(
    val wallets: ImmutableList<UserWalletItemUM>,
    val selectedUserWalletId: UserWalletId,
)