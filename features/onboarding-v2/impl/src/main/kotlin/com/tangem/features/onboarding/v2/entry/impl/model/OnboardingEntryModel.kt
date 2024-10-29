package com.tangem.features.onboarding.v2.entry.impl.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.model.state.OnboardingState
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ComponentScoped
internal class OnboardingEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val params: OnboardingEntryComponent.Params,
) : Model() {

    val state = MutableStateFlow(
        OnboardingState(
            currentRoute = OnboardingRoute.Wallet12(params.scanResponse),
        ),
    )
}
