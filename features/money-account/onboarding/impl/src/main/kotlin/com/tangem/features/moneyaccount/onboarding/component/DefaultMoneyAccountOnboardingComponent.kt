package com.tangem.features.moneyaccount.onboarding.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.moneyaccount.onboarding.model.MoneyAccountOnboardingModel
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMoneyAccountOnboardingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: MoneyAccountOnboardingComponent.Params,
) : MoneyAccountOnboardingComponent, AppComponentContext by appComponentContext {

    private val model: MoneyAccountOnboardingModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        MoneyAccountOnboardingScreen(modifier = modifier, state = state)
    }

    @AssistedFactory
    interface Factory : MoneyAccountOnboardingComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MoneyAccountOnboardingComponent.Params,
        ): DefaultMoneyAccountOnboardingComponent
    }
}