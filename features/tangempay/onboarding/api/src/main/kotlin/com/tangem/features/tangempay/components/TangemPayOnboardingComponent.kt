package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface TangemPayOnboardingComponent : ComposableContentComponent {

    sealed class Params {

        data class Deeplink(
            val deeplink: String,
        ) : Params()

        data class ContinueOnboarding(
            val userWalletId: UserWalletId,
        ) : Params()

        data class FromBannerOnMain(
            val userWalletId: UserWalletId,
        ) : Params()

        data object FromBannerInSettings : Params()
    }

    interface Factory : ComponentFactory<Params, TangemPayOnboardingComponent>
}