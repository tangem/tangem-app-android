package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.features.wallet.redux.WalletData
import org.rekotlin.Action

sealed class TokensAction : Action {

    object LoadCurrencies : TokensAction() {
        data class Success(val currencies: List<CurrencyListItem>) : TokensAction()
    }

    object LoadCardTokens : TokensAction() {
        data class Success(val tokens: List<Token>) : TokensAction()
    }

    data class SetAddedCurrencies(val wallets: List<WalletData>) : TokensAction()

    data class ToggleShowTokensForBlockchain(val isShown: Boolean, val blockchain: Blockchain) : TokensAction()

}