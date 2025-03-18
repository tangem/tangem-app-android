package com.tangem.features.onboarding.v2.twin.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
class OnboardingTwinModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {
    // TODO
}