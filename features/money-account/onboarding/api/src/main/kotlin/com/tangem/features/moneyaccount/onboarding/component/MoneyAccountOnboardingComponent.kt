package com.tangem.features.moneyaccount.onboarding.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface MoneyAccountOnboardingComponent : ComposableContentComponent {

    sealed class Params {

        abstract val userWalletId: UserWalletId

        data class Deeplink(override val userWalletId: UserWalletId, val deeplink: String) : Params()

        data class FromMain(override val userWalletId: UserWalletId) : Params()

        data class FromDetailsScreen(override val userWalletId: UserWalletId) : Params()
    }

    interface Factory : ComponentFactory<Params, MoneyAccountOnboardingComponent>
}