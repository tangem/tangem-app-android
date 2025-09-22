package com.tangem.features.managetokens.entity.managetokens

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class ManageTokensBottomSheetConfig {

    @Serializable
    data class AddWalletCustomToken(
        val userWalletId: UserWalletId,
    ) : ManageTokensBottomSheetConfig()

    @Serializable
    data class AddAccountCustomToken(
        val accountId: AccountId,
    ) : ManageTokensBottomSheetConfig()
}