package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.SingleWalletCardStateConverter
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.SingleWalletMarketPriceConverter
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.VisaWalletCardStateConverter
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
            is WalletState.Visa.Content -> {
                prevState.copy(
                    walletCardState = prevState.walletCardState.toLoadedVisaState(),
                    depositButtonState = prevState.depositButtonState.copy(isEnabled = true),
                )
            }
            is WalletState.Visa.Locked,
            is WalletState.SingleCurrency.Locked,
            -> {
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

    private fun WalletCardState.toLoadedVisaState(): WalletCardState {
        return VisaWalletCardStateConverter(status, userWallet, appCurrency).convert(value = this)
    }

    private fun MarketPriceBlockState.toLoadedState(): MarketPriceBlockState {
        return SingleWalletMarketPriceConverter(status.value, appCurrency).convert(value = this)
    }
}