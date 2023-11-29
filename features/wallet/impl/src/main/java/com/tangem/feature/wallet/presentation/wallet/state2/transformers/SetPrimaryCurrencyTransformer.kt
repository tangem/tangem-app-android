package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.SingleWalletCardStateConverter
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.SingleWalletMarketPriceConverter
import timber.log.Timber

internal class SetPrimaryCurrencyTransformer(
    private val userWallet: UserWallet,
    private val status: CryptoCurrencyStatus.Status,
    private val appCurrency: AppCurrency,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(
                    walletCardState = prevState.walletCardState.toLoadedState(),
                    marketPriceBlockState = prevState.marketPriceBlockState.toLoadedState(),
                )
            }
            is WalletState.SingleCurrency.Locked,
            -> {
                Timber.e("Impossible to load primary currency status for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency,
            -> {
                Timber.e("Impossible to load primary currency status for multi-currency wallet")
                prevState
            }
        }
    }

    private fun WalletCardState.toLoadedState(): WalletCardState {
        return SingleWalletCardStateConverter(status, userWallet, appCurrency).convert(value = this)
    }

    private fun MarketPriceBlockState.toLoadedState(): MarketPriceBlockState {
        return SingleWalletMarketPriceConverter(status, appCurrency).convert(value = this)
    }
}