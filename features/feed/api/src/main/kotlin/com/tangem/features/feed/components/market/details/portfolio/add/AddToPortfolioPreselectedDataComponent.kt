package com.tangem.features.feed.components.market.details.portfolio.add

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface AddToPortfolioPreselectedDataComponent : ComposableBottomSheetComponent {

    /**
     * @param tokenToAdd token and preselected network (no network selector).
     * @param callback callbacks for add-to-portfolio flow.
     */
    data class Params(
        val tokenToAdd: TokenToAdd,
        val callback: Callback,
    )

    interface Callback {
        fun onDismiss()
        fun onSuccess(addedToken: CryptoCurrency, walletId: UserWalletId)
    }

    data class TokenToAdd(
        val network: TokenMarketInfo.Network,
        val id: CryptoCurrency.RawID,
        val name: String,
        val symbol: String,
    )

    interface Factory : ComponentFactory<Params, AddToPortfolioPreselectedDataComponent>
}