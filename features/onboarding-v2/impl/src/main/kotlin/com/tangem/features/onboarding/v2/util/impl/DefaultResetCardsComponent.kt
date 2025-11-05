package com.tangem.features.onboarding.v2.util.impl

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.onboarding.v2.util.ResetCardsComponent
import com.tangem.features.onboarding.v2.util.impl.model.ResetCardsModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultResetCardsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: ResetCardsComponent.Params,
) : ResetCardsComponent, AppComponentContext by context {

    private val model: ResetCardsModel = getOrCreateModel(params)

    override fun startResetCardsFlow(createdUserWallet: UserWallet.Cold) {
        model.startResetCardsFlow(createdUserWallet)
    }

    @AssistedFactory
    interface Factory : ResetCardsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ResetCardsComponent.Params,
        ): DefaultResetCardsComponent
    }
}