package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPaySelectPlanComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val mode: Mode,
    )

    sealed interface Mode {
        data object TiersOnboarding : Mode
        data object ChangePlan : Mode
        data class PickPlan(val onPlanSelected: (TangemPayTariffPlanTransition) -> Unit) : Mode
    }

    interface Factory : ComponentFactory<Params, TangemPaySelectPlanComponent>
}