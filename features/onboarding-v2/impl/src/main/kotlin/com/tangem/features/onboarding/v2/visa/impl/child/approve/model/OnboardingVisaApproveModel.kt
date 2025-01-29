package com.tangem.features.onboarding.v2.visa.impl.child.approve.model

import androidx.compose.runtime.Stable
import com.tangem.common.CompletionResult
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onboarding.v2.visa.impl.child.approve.OnboardingVisaApproveComponent
import com.tangem.features.onboarding.v2.visa.impl.child.approve.ui.state.OnboardingVisaApproveUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ComponentScoped
internal class OnboardingVisaApproveModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
) : Model() {

    private val params = paramsContainer.require<OnboardingVisaApproveComponent.Config>()
    private val _uiState = MutableStateFlow(getInitialState())

    val uiState = _uiState.asStateFlow()
    val onDone = MutableSharedFlow<Unit>()

    private fun getInitialState(): OnboardingVisaApproveUM {
        return OnboardingVisaApproveUM(
            onApproveClick = ::onApproveClick,
        )
    }

    private fun onApproveClick() {
        loading(true)

        modelScope.launch {
            val result = tangemSdkManager.visaCustomerWalletApprove(
                visaDataForApprove = params.visaDataForApprove,
            )

            when (result) {
                is CompletionResult.Success -> {
                    // TODO make backend call

                    onDone.emit(Unit)
                }
                is CompletionResult.Failure -> {
                    loading(false)
                    // TODO show dialog
                    return@launch
                }
            }
        }
    }

    private fun loading(state: Boolean) {
        _uiState.update { it.copy(approveButtonLoading = state) }
    }
}