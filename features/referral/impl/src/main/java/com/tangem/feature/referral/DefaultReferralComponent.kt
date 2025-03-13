package com.tangem.feature.referral

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.feature.referral.api.ReferralComponent
import com.tangem.feature.referral.model.ReferralModel
import com.tangem.feature.referral.ui.ReferralScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DefaultReferralComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: ReferralComponent.Params,
) : ReferralComponent, AppComponentContext by appComponentContext {

    private val model: ReferralModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        ReferralScreen(stateHolder = model.uiState)
    }

    @AssistedFactory
    interface Factory : ReferralComponent.Factory {
        override fun create(context: AppComponentContext, params: ReferralComponent.Params): DefaultReferralComponent
    }
}