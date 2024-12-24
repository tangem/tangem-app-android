package com.tangem.features.onboarding.v2.visa.impl.child.accesscode.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.OnboardingVisaAccessCodeComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaAccessCodeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val paramsContainer: ParamsContainer,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<OnboardingVisaAccessCodeComponent.Params>()

    fun onBack() {
        TODO()
    }
}