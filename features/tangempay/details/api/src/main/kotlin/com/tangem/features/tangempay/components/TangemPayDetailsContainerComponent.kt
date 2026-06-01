package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.AccountStatus

interface TangemPayDetailsContainerComponent : ComposableContentComponent {
    data class Params(val initialStatus: AccountStatus.Payment)
    interface Factory : ComponentFactory<Params, TangemPayDetailsContainerComponent>
}