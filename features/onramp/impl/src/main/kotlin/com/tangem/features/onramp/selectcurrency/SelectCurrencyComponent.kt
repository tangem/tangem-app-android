package com.tangem.features.onramp.selectcurrency

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet

internal interface SelectCurrencyComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrency: CryptoCurrency,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, SelectCurrencyComponent>
}