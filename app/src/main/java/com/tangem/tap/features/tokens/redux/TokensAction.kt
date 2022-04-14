package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.tap.domain.tokens.Currency
import com.tangem.tap.features.wallet.redux.WalletData
import org.rekotlin.Action

sealed class TokensAction : Action {

    object ResetState : TokensAction()

    data class AllowToAddTokens(val allow: Boolean) : TokensAction()

    data class LoadCurrencies(val supportedBlockchains: List<Blockchain>? = null) : TokensAction() {
        data class Success(val currencies: List<Currency>) : TokensAction()
    }

    data class SetAddedCurrencies(
        val wallets: List<WalletData>, val derivationStyle: DerivationStyle?
    ) : TokensAction()
    data class SetNonRemovableCurrencies(val wallets: List<WalletData>) : TokensAction()

    data class SaveChanges(
        val addedTokens: List<TokenWithBlockchain>,
        val addedBlockchains: List<Blockchain>
    ) : TokensAction()

    object PrepareAndNavigateToAddCustomToken : TokensAction()
}