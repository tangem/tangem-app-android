package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.ui.TangemPayOnboardingScreenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<TangemPayOnboardingComponent.Params>()

    val screenState: StateFlow<TangemPayOnboardingScreenState>
    field = MutableStateFlow(TangemPayOnboardingScreenState())
}