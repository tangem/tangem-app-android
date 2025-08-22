package com.tangem.features.send.v2.api.subcomponents.destination

import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM

interface SendDestinationComponent : ComposableContentComponent {

    fun updateState(destinationUM: DestinationUM)

    interface ModelCallback : NavigationModelCallback {
        fun onDestinationResult(destinationUM: DestinationUM)
    }

    interface Factory : ComponentFactory<SendDestinationComponentParams.DestinationParams, SendDestinationComponent>
}