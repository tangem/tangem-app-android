package com.tangem.features.managetokens.component

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

interface OnboardingManageTokensComponent : ComposableContentComponent {

    data class Params(val userWalletId: UserWalletId)

    interface Factory {
        fun create(context: AppComponentContext, params: Params, onDone: () -> Unit): OnboardingManageTokensComponent
    }
}