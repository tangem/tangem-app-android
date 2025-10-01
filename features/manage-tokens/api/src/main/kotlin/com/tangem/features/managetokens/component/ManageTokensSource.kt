package com.tangem.features.managetokens.component

import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId

enum class ManageTokensSource(val analyticsName: String) {
    STORIES(analyticsName = "Stories"),
    ONBOARDING(analyticsName = "Onboarding"),
    SETTINGS(analyticsName = "Settings"),
    SEND_VIA_SWAP(analyticsName = "SendViaSwap"),
}

sealed interface ManageTokensMode {
    data class Wallet(val userWalletId: UserWalletId) : ManageTokensMode
    data class Account(val accountId: AccountId) : ManageTokensMode
    data object None : ManageTokensMode
}

sealed interface AddCustomTokenMode {
    data class Wallet(val userWalletId: UserWalletId) : AddCustomTokenMode
    data class Account(val accountId: AccountId) : AddCustomTokenMode
}