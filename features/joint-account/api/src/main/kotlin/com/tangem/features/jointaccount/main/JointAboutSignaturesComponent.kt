package com.tangem.features.jointaccount.main

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface JointAboutSignaturesComponent : ComposableBottomSheetComponent {

    interface Factory : ComponentFactory<Params, JointAboutSignaturesComponent>

    data class Params(
        val threshold: Int,
        val total: Int,
        val onDismiss: () -> Unit,
    )
}