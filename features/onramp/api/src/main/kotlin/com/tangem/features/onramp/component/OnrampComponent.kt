package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface OnrampComponent : ComposableContentComponent {

    data class Params(val userWalletId: UserWalletId, val cryptoCurrency: CryptoCurrency)

    interface Factory : ComponentFactory<Params, OnrampComponent>
}
