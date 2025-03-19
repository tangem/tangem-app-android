package com.tangem.features.onboarding.v2.twin.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class OnboardingTwinModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    val params = paramsContainer.require<OnboardingTwinComponent.Params>()

    // TODO
}