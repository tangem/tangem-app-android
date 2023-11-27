package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.domain.wallets.models.UserWalletId
import javax.inject.Inject

/**
* [REDACTED_AUTHOR]
 */
internal interface WalletCardClickIntents {

    fun onRenameClick(userWalletId: UserWalletId, name: String)

    fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId)

    fun onDeleteAfterConfirmationClick(userWalletId: UserWalletId)
}

internal class WalletCardClickIntentsImplementor @Inject constructor() : WalletCardClickIntents {

    override fun onRenameClick(userWalletId: UserWalletId, name: String) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onDeleteBeforeConfirmationClick(userWalletId: UserWalletId) {
// [REDACTED_TODO_COMMENT]
    }

    override fun onDeleteAfterConfirmationClick(userWalletId: UserWalletId) {
// [REDACTED_TODO_COMMENT]
    }
}
