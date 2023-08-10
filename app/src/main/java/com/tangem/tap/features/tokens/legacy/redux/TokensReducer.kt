package com.tangem.tap.features.tokens.legacy.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.Currency.Token
import org.rekotlin.Action

object TokensReducer {
    fun reduce(action: Action, state: AppState): TokensState = internalReduce(action, state)
}

private fun internalReduce(action: Action, state: AppState): TokensState {
    if (action !is TokensAction) return state.tokensState

    return when (action) {
        is TokensAction.SetArgs.ManageAccess -> {
            state.tokensState.copy(
                isManageAccess = true,
                addedWallets = action.wallets,
                addedBlockchains = action.wallets.toNonCustomBlockchains(action.derivationStyle),
                addedTokens = action.wallets.toNonCustomTokensWithBlockchains(action.derivationStyle),
            )
        }

        is TokensAction.SetArgs.ReadAccess -> {
            state.tokensState.copy(isManageAccess = false)
        }

        else -> state.tokensState
    }
}

private fun List<WalletDataModel>.toNonCustomBlockchains(derivationStyle: DerivationStyle?): List<Blockchain> {
    return mapNotNull { walletDataModel ->
        if (walletDataModel.currency.isCustomCurrency(derivationStyle)) {
            null
        } else {
            (walletDataModel.currency as? Currency.Blockchain)?.blockchain
        }
    }.distinct()
}

private fun List<WalletDataModel>.toNonCustomTokensWithBlockchains(
    derivationStyle: DerivationStyle?,
): List<TokenWithBlockchain> {
    return mapNotNull { walletDataModel ->
        if (walletDataModel.currency !is Token) return@mapNotNull null
        if (walletDataModel.currency.isCustomCurrency(derivationStyle)) return@mapNotNull null

        TokenWithBlockchain(walletDataModel.currency.token, walletDataModel.currency.blockchain)
    }.distinct()
}
