package com.tangem.features.onramp.confirmresidency

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampCountry

internal interface ConfirmResidencyComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val country: OnrampCountry,
        val isLaunchSepa: Boolean,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, ConfirmResidencyComponent>
}