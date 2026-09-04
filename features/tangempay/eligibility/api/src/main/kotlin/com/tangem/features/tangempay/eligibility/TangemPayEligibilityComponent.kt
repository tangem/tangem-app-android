package com.tangem.features.tangempay.eligibility

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPayEligibilityComponent : ComposableContentComponent {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : ComponentFactory<Params, TangemPayEligibilityComponent>
}