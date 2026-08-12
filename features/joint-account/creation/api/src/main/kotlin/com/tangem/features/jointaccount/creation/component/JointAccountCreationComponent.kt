package com.tangem.features.jointaccount.creation.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface JointAccountCreationComponent : ComposableContentComponent {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : ComponentFactory<Params, JointAccountCreationComponent>
}