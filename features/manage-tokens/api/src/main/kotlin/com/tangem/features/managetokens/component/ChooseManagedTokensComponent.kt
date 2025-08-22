package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface ChooseManagedTokensComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val initialCurrency: CryptoCurrency,
        val selectedCurrency: CryptoCurrency?,
        val source: Source,
    )

    enum class Source {
        SendViaSwap,
    }

    interface Factory : ComponentFactory<Params, ChooseManagedTokensComponent>
}