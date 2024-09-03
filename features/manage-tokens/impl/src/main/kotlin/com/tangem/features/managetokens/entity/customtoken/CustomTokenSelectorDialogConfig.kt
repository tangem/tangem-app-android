package com.tangem.features.managetokens.entity.customtoken

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class CustomTokenSelectorDialogConfig {

    @Serializable
    data class CustomDerivationInput(
        val userWalletId: UserWalletId,
    ) : CustomTokenSelectorDialogConfig()
}