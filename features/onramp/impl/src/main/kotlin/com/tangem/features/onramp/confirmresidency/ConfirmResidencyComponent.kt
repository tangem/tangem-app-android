package com.tangem.features.onramp.confirmresidency

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

internal interface ConfirmResidencyComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val country: OnrampCountry,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, ConfirmResidencyComponent>
}