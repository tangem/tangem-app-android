package com.tangem.features.onboarding.v2.note.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.note.api.OnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.OnboardingNoteInnerNavigationState
import com.tangem.features.onboarding.v2.note.impl.route.OnboardingNoteRoute
import com.tangem.features.onboarding.v2.note.impl.route.stepNum
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ModelScoped
internal class OnboardingNoteModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<OnboardingNoteComponent.Params>()

    private val _currentScanResponse = MutableStateFlow(params.scanResponse)

    val initialRoute = OnboardingNoteRoute.CreateWallet // TODO [REDACTED_TASK_KEY]

    private val _innerNavigationState =
        MutableStateFlow(OnboardingNoteInnerNavigationState(stackSize = initialRoute.stepNum()))

    val currentScanResponse = _currentScanResponse.asStateFlow()

    val innerNavigationState = _innerNavigationState.asStateFlow()

    val onDone = MutableSharedFlow<Unit>()

    val stackNavigation = StackNavigation<OnboardingNoteRoute>()

    private val _uiState = MutableStateFlow(
        OnboardingNoteState(),
    )

    val uiState: StateFlow<OnboardingNoteState> = _uiState

    fun updateStepForNewRoute(route: OnboardingNoteRoute) {
        _innerNavigationState.value = innerNavigationState.value.copy(
            stackSize = route.stepNum(),
        )
    }

    @Suppress("UnusedPrivateMember")
    fun onChildBack(currentRoute: OnboardingNoteRoute) {
        // TODO [REDACTED_TASK_KEY]
    }
}