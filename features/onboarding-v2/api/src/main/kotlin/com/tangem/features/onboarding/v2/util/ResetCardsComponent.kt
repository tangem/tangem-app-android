package com.tangem.features.onboarding.v2.util

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.domain.models.wallet.UserWallet

interface ResetCardsComponent {

    fun startResetCardsFlow(createdUserWallet: UserWallet.Cold)

    data class Params(
        val callbacks: ModelCallbacks,
    )

    interface ModelCallbacks {
        fun onCancel()
        fun onComplete()
    }

    interface Factory : ComponentFactory<Params, ResetCardsComponent>
}