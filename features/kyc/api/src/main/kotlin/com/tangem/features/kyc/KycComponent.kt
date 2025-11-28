package com.tangem.features.kyc

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface KycComponent : ComposableContentComponent {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : ComponentFactory<Params, KycComponent>
}