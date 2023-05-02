package com.tangem.tap.features.tokens.legacy.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.tokens.Currency
import org.rekotlin.Action

sealed class TokensAction : Action {

    object ResetState : TokensAction()

    data class AllowToAddTokens(val allow: Boolean) : TokensAction()

    data class LoadCurrencies(val scanResponse: ScanResponse? = null) : TokensAction() {
        data class Success(val currencies: List<Currency>, val loadMore: Boolean) : TokensAction()
        object Failure : TokensAction()
    }

    data class LoadMore(
        val scanResponse: ScanResponse? = null,
    ) : TokensAction()

    data class SetAddedCurrencies(
        val wallets: List<WalletDataModel>,
        val derivationStyle: DerivationStyle?,
    ) : TokensAction()

    data class SaveChanges(
        val addedTokens: List<TokenWithBlockchain>,
        val addedBlockchains: List<Blockchain>,
    ) : TokensAction()

    object PrepareAndNavigateToAddCustomToken : TokensAction()

    data class SetSearchInput(val searchInput: String) : TokensAction()
}