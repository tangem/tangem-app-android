package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.features.wallet.redux.WalletData
import org.rekotlin.Action

sealed class TokensAction : Action {

    object ResetState : TokensAction()

    object LoadCurrencies : TokensAction() {
        data class Success(val currencies: List<CurrencyListItem>) : TokensAction()
    }

    object LoadCardTokens : TokensAction() {
        data class Success(val tokens: List<Token>) : TokensAction()
    }

    data class SetAddedCurrencies(val wallets: List<WalletData>) : TokensAction()

    data class ToggleShowTokensForBlockchain(val isShown: Boolean, val blockchain: Blockchain) : TokensAction()

    sealed class TokensList : TokensAction() {
        data class AddBlockchain(val blockchain: Blockchain) : TokensList()
        data class AddToken(val token: Token) : TokensList()
        object SaveChanges : TokensList()
    }
}