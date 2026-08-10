package com.tangem.feature.walletsettings.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface AddAccountTypeComponent : ComposableBottomSheetComponent {

    data class Params(
        val onCryptoAccountClick: () -> Unit,
        val onJointAccountClick: () -> Unit,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, AddAccountTypeComponent>
}