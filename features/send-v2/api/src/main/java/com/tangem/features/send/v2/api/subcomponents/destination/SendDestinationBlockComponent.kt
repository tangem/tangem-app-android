package com.tangem.features.send.v2.api.subcomponents.destination

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface SendDestinationBlockComponent : ComposableContentComponent {

    interface Factory :
        ComponentFactory<SendDestinationComponentParams.DestinationBlockParams, SendDestinationBlockComponent>
}