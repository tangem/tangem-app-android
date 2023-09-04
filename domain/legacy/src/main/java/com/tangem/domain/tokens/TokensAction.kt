package com.tangem.domain.tokens

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import org.rekotlin.Action

sealed interface TokensAction : Action {

    /** Single way to pass data to the screen */
    sealed interface SetArgs : TokensAction {
        object ManageAccess : SetArgs
        object ReadAccess : SetArgs
    }

    @Deprecated("Action is used for saving data by old way. It will be removed after deleting of legacy wallet screen")
    data class LegacySaveChanges(
        val currentTokens: List<TokenWithBlockchain>,
        val currentBlockchains: List<Blockchain>,
        val changedTokens: List<TokenWithBlockchain>,
        val changedBlockchains: List<Blockchain>,
        val scanResponse: ScanResponse,
    ) : TokensAction

    data class NewSaveChanges(
        val currentTokens: List<CryptoCurrency.Token>,
        val currentCoins: List<CryptoCurrency.Coin>,
        val changedTokens: List<CryptoCurrency.Token>,
        val changedCoins: List<CryptoCurrency.Coin>,
        val userWallet: UserWallet,
    ) : TokensAction
}

data class TokenWithBlockchain(val token: Token, val blockchain: Blockchain)