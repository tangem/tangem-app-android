package com.tangem.feature.walletsettings.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface NetworksAvailableForNotificationsComponent : ComposableBottomSheetComponent {

    data class Params(
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, NetworksAvailableForNotificationsComponent>
}