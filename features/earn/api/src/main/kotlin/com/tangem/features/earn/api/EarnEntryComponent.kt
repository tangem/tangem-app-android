package com.tangem.features.earn.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface EarnEntryComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val source: EarnEntrySource,
        val start: EarnEntryStart,
    )

    interface Factory : ComponentFactory<Params, EarnEntryComponent>
}