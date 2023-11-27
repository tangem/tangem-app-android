package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletTokensListState
import timber.log.Timber

internal class SetTokenListErrorTransformer(
    userWalletId: UserWalletId,
    private val error: TokenListError,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (error) {
            is TokenListError.EmptyTokens -> {
                when (prevState) {
                    is WalletState.MultiCurrency.Content -> {
                        prevState.copy(tokensListState = WalletTokensListState.Empty)
                    }
                    is WalletState.MultiCurrency.Locked,
                    -> {
                        Timber.e("Impossible to load tokens list for locked wallet")
                        prevState
                    }
                    is WalletState.SingleCurrency,
                    -> {
                        Timber.e("Impossible to load tokens list for single-currency wallet")
                        prevState
                    }
                }
            }
            is TokenListError.DataError,
            is TokenListError.UnableToSortTokenList,
            -> prevState
        }
    }
}
