package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface TangemPayWaitlistComponent : ComposableBottomSheetComponent {

    data class Params(val onDismiss: () -> Unit)

    interface Factory : ComponentFactory<Params, TangemPayWaitlistComponent>
}