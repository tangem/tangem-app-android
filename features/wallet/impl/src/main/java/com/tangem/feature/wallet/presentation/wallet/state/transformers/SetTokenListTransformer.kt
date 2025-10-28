package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.MultiWalletCardStateConverter
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.TokenListStateConverter
import com.tangem.feature.wallet.presentation.wallet.state.utils.enableButtons
import timber.log.Timber

internal class SetTokenListTransformer(
    private val params: TokenConverterParams,
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntents,
    private val yieldSupplyApyMap: Map<String, String> = emptyMap(),
    private val stakingApyMap: Map<String, List<Yield.Validator>> = emptyMap(),
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
        val fiatBalance = when (params) {
            is TokenConverterParams.Account -> params.accountList.totalFiatBalance
            is TokenConverterParams.Wallet -> params.tokenList.totalFiatBalance
        }
        return MultiWalletCardStateConverter(
            fiatBalance = fiatBalance,
            selectedWallet = userWallet,
            appCurrency = appCurrency,
        ).convert(value = this)
    }

    private fun WalletTokensListState.toLoadedState(): WalletTokensListState {
        return TokenListStateConverter(
            params = params,
            selectedWallet = userWallet,
            appCurrency = appCurrency,
            clickIntents = clickIntents,
            yieldModuleApyMap = yieldSupplyApyMap,
            stakingApyMap = stakingApyMap,
        ).convert(value = this)
    }
}