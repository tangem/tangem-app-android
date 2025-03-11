package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.MultiWalletCardStateConverter
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.TokenListStateConverter
import com.tangem.feature.wallet.presentation.wallet.state.utils.enableButtons
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import timber.log.Timber

internal class SetTokenListTransformer(
    private val tokenList: TokenList,
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntents,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(
                    walletCardState = prevState.walletCardState.toLoadedState(),
                    tokensListState = prevState.tokensListState.toLoadedState(),
                    buttons = prevState.enableButtons(),
                )
            }
            is WalletState.MultiCurrency.Locked -> {
                Timber.w("Impossible to load tokens list for locked wallet")
                prevState
            }
            is WalletState.Visa,
            is WalletState.SingleCurrency,
            -> {
                Timber.w("Impossible to load tokens list for single-currency wallet")
                prevState
            }
        }
    }

    private fun WalletCardState.toLoadedState(): WalletCardState {
        return MultiWalletCardStateConverter(
            fiatBalance = tokenList.totalFiatBalance,
            selectedWallet = userWallet,
            appCurrency = appCurrency,
        ).convert(value = this)
    }

    private fun WalletTokensListState.toLoadedState(): WalletTokensListState {
        return TokenListStateConverter(
            tokenList = tokenList,
            selectedWallet = userWallet,
            appCurrency = appCurrency,
            clickIntents = clickIntents,
        ).convert(value = this)
    }
}