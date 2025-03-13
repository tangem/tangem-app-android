package com.tangem.features.onboarding.v2.note.impl.child.create.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.note.impl.child.create.ui.state.OnboardingNoteCreateWalletUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
@ModelScoped
internal class OnboardingNoteCreateWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
) : Model() {

    private val _uiState = MutableStateFlow(
        OnboardingNoteCreateWalletUM(),
    )

    val uiState: StateFlow<OnboardingNoteCreateWalletUM> = _uiState
}