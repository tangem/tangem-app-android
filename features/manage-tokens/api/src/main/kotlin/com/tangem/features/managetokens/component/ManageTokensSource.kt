package com.tangem.features.managetokens.component

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

enum class ManageTokensSource(val analyticsName: String) {
    STORIES(analyticsName = "Stories"),
    ONBOARDING(analyticsName = "Onboarding"),
    SETTINGS(analyticsName = "Wallet Settings"),
    ACCOUNT(analyticsName = "Account"),
    TOKEN_SYNC_BANNER(analyticsName = "Token Sync Banner"),
    SEND_VIA_SWAP(analyticsName = "SendViaSwap"),
}

sealed interface ManageTokensMode {
    data class Account(val accountId: AccountId) : ManageTokensMode {
        constructor(userWalletId: UserWalletId) : this(AccountId.forMainCryptoPortfolio(userWalletId))
    }
    data object None : ManageTokensMode
}

@Serializable
data class AddCustomTokenMode(val accountId: AccountId) {
    val userWalletId: UserWalletId = accountId.userWalletId

    constructor(userWalletId: UserWalletId) : this(AccountId.forMainCryptoPortfolio(userWalletId))
}