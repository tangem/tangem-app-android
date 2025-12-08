package com.tangem.features.tokendetails

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.details.NavigationAction

interface TokenDetailsComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val navigationAction: NavigationAction? = null,
    )

    interface Factory : ComponentFactory<Params, TokenDetailsComponent>
}