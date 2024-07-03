package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListState
import timber.log.Timber
import java.math.BigDecimal

internal class SetTokenListErrorTransformer(
    private val selectedWallet: UserWallet,
    private val error: TokenListError,
    private val appCurrency: AppCurrency,
) : WalletStateTransformer(selectedWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (error) {
            is TokenListError.EmptyTokens -> {
                when (prevState) {
                    is WalletState.MultiCurrency.Content -> {
                        prevState.copy(
                            walletCardState = prevState.walletCardState.toLoadedState(),
                            tokensListState = WalletTokensListState.Empty,
                        )
                    }
                    is WalletState.MultiCurrency.Locked -> {
                        Timber.w("Impossible to load tokens list for locked wallet")
                        prevState
                    }
                    is WalletState.SingleCurrency -> {
                        Timber.w("Impossible to load tokens list for single-currency wallet")
                        prevState
                    }
                    is WalletState.Visa -> {
                        Timber.w("Impossible to load tokens list for VISA wallet")
                        prevState
                    }
                }
            }
            is TokenListError.DataError,
            is TokenListError.UnableToSortTokenList,
            -> prevState
        }
    }

    private fun WalletCardState.toLoadedState(): WalletCardState {
        return WalletCardState.Content(
            id = id,
            title = title,
            additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = selectedWallet),
            imageResId = imageResId,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
            balance = BigDecimalFormatter.formatFiatAmount(
                fiatAmount = BigDecimal.ZERO,
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            ),
            cardCount = selectedWallet.getCardsCount(),
        )
    }
}
