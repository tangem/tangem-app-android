package com.tangem.features.managetokens.entity.managetokens

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class ManageTokensBottomSheetConfig {

    @Serializable
    data class AddCustomToken(
        val userWalletId: UserWalletId,
    ) : ManageTokensBottomSheetConfig()
}