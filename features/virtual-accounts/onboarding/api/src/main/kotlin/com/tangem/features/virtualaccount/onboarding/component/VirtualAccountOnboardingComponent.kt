package com.tangem.features.virtualaccount.onboarding.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface VirtualAccountOnboardingComponent : ComposableContentComponent {

    sealed class Params {

        abstract val userWalletId: UserWalletId

        data class Deeplink(override val userWalletId: UserWalletId, val deeplink: String) : Params()

        data class FromMain(override val userWalletId: UserWalletId) : Params()

        data class FromDetailsScreen(override val userWalletId: UserWalletId) : Params()
    }

    interface Factory : ComponentFactory<Params, VirtualAccountOnboardingComponent>
}