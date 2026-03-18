package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.MultiWalletBalanceUMTransformer
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.MultiWalletCardStateConverter
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.TokenListStateConverter
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.WalletTokensListUMConverter
import com.tangem.feature.wallet.presentation.wallet.state.utils.enableButtons
import timber.log.Timber
import java.math.BigDecimal

internal class SetTokenListTransformer(
    private val params: TokenConverterParams,
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val clickIntents: WalletClickIntents,
    private val yieldSupplyApyMap: Map<String, BigDecimal> = emptyMap(),
    private val stakingAvailabilityMap: Map<CryptoCurrency, StakingAvailability> = emptyMap(),
    private val shouldShowMainPromo: Boolean,
    private val isAccountsModeEnabled: Boolean,
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
            is WalletState.SingleCurrency,
            -> {
                Timber.w("Impossible to load tokens list for single-currency wallet")
                prevState
            }
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return when (walletUM) {
            is WalletUM.Content -> {
                walletUM.copy(
                    walletsBalanceUM = walletUM.walletsBalanceUM.toLoadedState2(),
                    tokensListUM = toLoadedState(),
                    buttons = walletUM.enableButtons(),
                )
            }
            is WalletUM.Locked -> {
                Timber.w("Impossible to load tokens list for locked wallet")
                walletUM
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

    private fun WalletBalanceUM.toLoadedState2(): WalletBalanceUM {
        val fiatBalance = when (params) {
            is TokenConverterParams.Account -> params.accountList.totalFiatBalance
            is TokenConverterParams.Wallet -> params.tokenList.totalFiatBalance
        }
        return MultiWalletBalanceUMTransformer(
            fiatBalance = fiatBalance,
            appCurrency = appCurrency,
        ).transform(prevState = this)
    }

    private fun WalletTokensListState.toLoadedState(): WalletTokensListState {
        return TokenListStateConverter(
            params = params,
            selectedWallet = userWallet,
            appCurrency = appCurrency,
            clickIntents = clickIntents,
            yieldModuleApyMap = yieldSupplyApyMap,
            stakingAvailabilityMap = stakingAvailabilityMap,
            shouldShowMainPromo = shouldShowMainPromo,
        ).convert(value = this)
    }

    private fun toLoadedState(): WalletTokensListUM {
        if (params !is TokenConverterParams.Account) return WalletTokensListUM.Empty

        return WalletTokensListUMConverter(
            selectedWallet = userWallet,
            appCurrency = appCurrency,
            clickIntents = clickIntents,
            yieldModuleApyMap = yieldSupplyApyMap,
            stakingAvailabilityMap = stakingAvailabilityMap,
            shouldShowMainPromo = shouldShowMainPromo,
            isAccountsModeEnabled = isAccountsModeEnabled,
            expandedAccounts = params.expandedAccounts,
        ).convert(value = params.accountList)
    }
}