package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPayOnboardingComponent : ComposableContentComponent {

    sealed class Params {

        abstract val userWalletId: UserWalletId?

        data class Deeplink(
            val deeplink: String,
            override val userWalletId: UserWalletId?,
        ) : Params()

        data class ContinueOnboarding(
            override val userWalletId: UserWalletId?,
        ) : Params()
    }

    interface Factory : ComponentFactory<Params, TangemPayOnboardingComponent>
}