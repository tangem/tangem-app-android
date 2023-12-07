package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state2.model.ManageTokensButtonConfig
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.MultiWalletCardStateConverter
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.TokenListStateConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import timber.log.Timber

internal class SetTokenListTransformer(
    private val tokenList: TokenList,
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntentsV2,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(
                    walletCardState = prevState.walletCardState.toLoadedState(),
                    tokensListState = prevState.tokensListState.toLoadedState(),
                    manageTokensButtonConfig = createManageTokensButtonConfig(),
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

    private fun createManageTokensButtonConfig(): ManageTokensButtonConfig? {
        return if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
            null
        } else {
            ManageTokensButtonConfig(clickIntents::onManageTokensClick)
        }
    }
}