package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.wallet.redux.WalletData
import org.rekotlin.Action

sealed class TokensAction : Action {

    object ResetState : TokensAction()

    object LoadCurrencies : TokensAction() {
        data class Success(val currencies: List<CurrencyListItem>) : TokensAction()
    }

    data class SetAddedCurrencies(val wallets: List<WalletData>) : TokensAction()

    data class ToggleShowTokensForBlockchain(val isShown: Boolean, val blockchain: Blockchain) : TokensAction()
    object OpenAllTokens : TokensAction()

    data class SaveChanges(val addedItems: List<CurrencyListItem>) : TokensAction()
}