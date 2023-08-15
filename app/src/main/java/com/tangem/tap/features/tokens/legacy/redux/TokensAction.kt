package com.tangem.tap.features.tokens.legacy.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.tap.domain.model.WalletDataModel
import org.rekotlin.Action

sealed interface TokensAction : Action {

    /** Single way to pass data to the screen */
    sealed interface SetArgs : TokensAction {

        data class ManageAccess(val wallets: List<WalletDataModel>, val derivationStyle: DerivationStyle?) : SetArgs

        object ReadAccess : SetArgs
    }

    // TODO: [REDACTED_TASK_KEY] Remove this action
    data class SaveChanges(val tokens: List<TokenWithBlockchain>, val blockchains: List<Blockchain>) : TokensAction

    // TODO: Remove this action in 4.7 release
    object PrepareAndNavigateToAddCustomToken : TokensAction
}