package com.tangem.features.onramp.hottokens.portfolio

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Onramp bottom sheet component for adding token to portfolio
 *
[REDACTED_AUTHOR]
 */
interface OnrampAddToPortfolioComponent : ComposableBottomSheetComponent {

    /**
     * Params
     *
     * @property userWalletId      user wallet id
     * @property cryptoCurrency    crypto currency
     * @property currencyIconState currency icon state
     * @property onSuccessAdding   lambda be invoked when token will added
     * @property onDismiss         lambda be invoked when bottom sheet is dismissed
     */
    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val currencyIconState: CurrencyIconState,
        val onSuccessAdding: (CryptoCurrency.ID) -> Unit,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, OnrampAddToPortfolioComponent>
}