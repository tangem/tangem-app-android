package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath

internal interface CustomTokenDerivationInputComponent : ComposableDialogComponent {

    data class Params(
        val mode: AddCustomTokenMode,
        val onConfirm: (SelectedDerivationPath) -> Unit,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, CustomTokenDerivationInputComponent>
}