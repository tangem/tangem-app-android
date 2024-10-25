package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal interface ConfirmResidencyComponent : ComposableBottomSheetComponent {

    data class Params(
        val countryName: String,
        val isOnrampSupported: Boolean,
        val countryFlagUrl: String,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, ConfirmResidencyComponent>
}
