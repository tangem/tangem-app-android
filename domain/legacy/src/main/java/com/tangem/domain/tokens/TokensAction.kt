package com.tangem.domain.tokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import org.rekotlin.Action

sealed interface TokensAction : Action {

    /** Single way to pass data to the screen */
    sealed interface SetArgs : TokensAction {
        object ManageAccess : SetArgs
        object ReadAccess : SetArgs
    }

    data class SaveChanges(
        val currentTokens: List<CryptoCurrency.Token>,
        val currentCoins: List<CryptoCurrency.Coin>,
        val changedTokens: List<CryptoCurrency.Token>,
        val changedCoins: List<CryptoCurrency.Coin>,
        val userWallet: UserWallet,
    ) : TokensAction
}

data class TokenWithBlockchain(val token: Token, val blockchain: Blockchain)