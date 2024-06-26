package com.tangem.feature.walletsettings.entity

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface DialogConfig {

    @Serializable
    data class RenameWallet(
        val userWalletId: UserWalletId,
        val currentName: String,
    ) : DialogConfig
}
