package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface AddCustomTokenComponent : ComposableBottomSheetComponent {

    data class Params(
        val mode: AddCustomTokenMode,
        val source: ManageTokensSource,
        val onDismiss: () -> Unit,
        val onCurrencyAdded: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, AddCustomTokenComponent>
}