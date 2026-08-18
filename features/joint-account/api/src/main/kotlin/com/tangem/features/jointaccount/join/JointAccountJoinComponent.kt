package com.tangem.features.jointaccount.join

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface JointAccountJoinComponent : ComposableContentComponent {

    /**
     * @property inviteId one-time invite secret extracted from the invite link
     */
    data class Params(val inviteId: String)

    interface Factory : ComponentFactory<Params, JointAccountJoinComponent>
}