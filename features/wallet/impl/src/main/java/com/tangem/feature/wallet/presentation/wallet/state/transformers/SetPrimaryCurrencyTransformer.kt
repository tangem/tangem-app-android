package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.SingleWalletCardStateConverter
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.SingleWalletMarketPriceConverter
import timber.log.Timber

internal class SetPrimaryCurrencyTransformer(
    private val userWallet: UserWallet,
    private val status: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(
                    walletCardState = prevState.walletCardState.toLoadedSingleCurrencyState(),
                    marketPriceBlockState = prevState.marketPriceBlockState.toLoadedState(),
                )
            }
            is WalletState.Visa -> {
                Timber.w("Impossible to load primary currency status for VISA wallet")
                prevState
            }
            is WalletState.SingleCurrency.Locked -> {
                Timber.w("Impossible to load primary currency status for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                Timber.w("Impossible to load primary currency status for multi-currency wallet")
                prevState
            }
        }
    }

    private fun WalletCardState.toLoadedSingleCurrencyState(): WalletCardState {
        return SingleWalletCardStateConverter(status.value, userWallet, appCurrency).convert(value = this)
    }

    private fun MarketPriceBlockState.toLoadedState(): MarketPriceBlockState {
        return SingleWalletMarketPriceConverter(status.value, appCurrency).convert(value = this)
    }
}