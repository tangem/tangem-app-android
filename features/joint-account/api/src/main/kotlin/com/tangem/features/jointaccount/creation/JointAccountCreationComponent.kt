package com.tangem.features.jointaccount.creation

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface JointAccountCreationComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val source: AppRoute.JointAccountCreation.Source,
    )

    interface Factory : ComponentFactory<Params, JointAccountCreationComponent>
}