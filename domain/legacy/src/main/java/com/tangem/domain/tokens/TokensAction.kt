package com.tangem.domain.tokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import org.rekotlin.Action

sealed interface TokensAction : Action {

    /** Single way to pass data to the screen */
    sealed interface SetArgs : TokensAction {
        object ManageAccess : SetArgs
        object ReadAccess : SetArgs
    }
}

data class TokenWithBlockchain(val token: Token, val blockchain: Blockchain)