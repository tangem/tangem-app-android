package com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.OnboardingVisaOtherWalletComponent
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.ui.state.OnboardingVisaOtherWalletUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaOtherWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val config = paramsContainer.require<OnboardingVisaOtherWalletComponent.Config>()
    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<Unit>()

    private fun getInitialState(): OnboardingVisaOtherWalletUM {
        return OnboardingVisaOtherWalletUM(
            onShareClick = ::onShareClicked,
            onOpenInBrowserClick = ::onOpenInBrowserClicked,
        )
    }

    private fun onShareClicked() {
        // TODO
    }

    private fun onOpenInBrowserClicked() {
        // TODO
    }
}