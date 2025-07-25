package com.tangem.features.send.v2.api.subcomponents.destination

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM

interface SendDestinationBlockComponent : ComposableContentComponent {

    fun updateState(destinationUM: DestinationUM)

    interface Factory {
        fun create(
            context: AppComponentContext,
            params: SendDestinationComponentParams.DestinationBlockParams,
            onClick: () -> Unit,
            onResult: (DestinationUM) -> Unit,
        ): SendDestinationBlockComponent
    }
}