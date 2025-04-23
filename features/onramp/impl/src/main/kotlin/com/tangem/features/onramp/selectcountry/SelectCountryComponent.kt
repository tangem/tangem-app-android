package com.tangem.features.onramp.selectcountry

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

internal interface SelectCountryComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val onDismiss: (Boolean) -> Unit,
    )

    interface Factory : ComponentFactory<Params, SelectCountryComponent>
}