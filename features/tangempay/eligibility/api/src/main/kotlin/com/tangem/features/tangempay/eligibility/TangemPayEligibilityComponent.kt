package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.pay.TangemPayProduct

interface TangemPayEligibilityComponent : ComposableContentComponent {

    data class Params(val product: TangemPayProduct)

    interface Factory : ComponentFactory<Params, TangemPayEligibilityComponent>
}