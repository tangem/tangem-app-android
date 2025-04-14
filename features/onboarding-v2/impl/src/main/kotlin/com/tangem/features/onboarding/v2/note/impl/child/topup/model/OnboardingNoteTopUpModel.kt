package com.tangem.features.onboarding.v2.note.impl.child.topup.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.note.impl.child.topup.ui.state.OnboardingNoteTopUpUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
@ModelScoped
internal class OnboardingNoteTopUpModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val _uiState = MutableStateFlow(
        OnboardingNoteTopUpUM(),
    )

    val uiState: StateFlow<OnboardingNoteTopUpUM> = _uiState
}