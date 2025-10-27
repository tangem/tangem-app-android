package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.ui.TangemPayOnboardingScreenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val repository: OnboardingRepository,
    private val produceInitialDataUseCase: ProduceTangemPayInitialDataUseCase,
) : Model() {

    private val params = paramsContainer.require<TangemPayOnboardingComponent.Params>()
    val uiState: StateFlow<TangemPayOnboardingScreenState>
        field = MutableStateFlow(getInitialState())

    init {
        modelScope.launch {
            when (params) {
                is TangemPayOnboardingComponent.Params.ContinueOnboarding -> {
                    checkCustomerInfo()
                }
                is TangemPayOnboardingComponent.Params.Deeplink -> {
                    repository.validateDeeplink(params.deeplink)
                        .onRight { isValid -> if (isValid) showOnboarding() }
                        .onLeft { back() }
                }
            }
        }
    }

    private fun showOnboarding() {
        uiState.update { it.copy(fullScreenLoading = false) }
    }

    private suspend fun checkCustomerInfo() {
        repository.getCustomerInfo()
            .onRight { customerInfo ->
                when {
                    !customerInfo.isKycApproved -> {
                        when (params) {
                            is TangemPayOnboardingComponent.Params.Deeplink ->
                                uiState.value = uiState.value.copy(fullScreenLoading = false)
                            else -> openKyc()
                        }
                    }
                    else -> back()
                }
            }
            .onLeft { back() }
    }

    private fun onGetCardClick() {
        uiState.update { it.copy(buttonLoading = true) }
        modelScope.launch {
            val result = produceInitialDataUseCase()
            if (result.isLeft()) {
                Timber.e("Error producing initial data: ${result.leftOrNull()?.message}")
                uiState.update { it.copy(buttonLoading = false) }
                return@launch
            }

            repository.getCustomerInfo()
                .fold(
                    ifLeft = {
                        Timber.e("Error getCustomerInfo: ${it.errorCode}")
                        uiState.update { it.copy(buttonLoading = false) }
                    },
                    ifRight = { customerInfo ->
                        if (customerInfo.isKycApproved) {
                            back()
                        } else {
                            openKyc()
                        }
                    },
                )
        }
    }

    private fun openKyc() {
        router.replaceAll(AppRoute.Wallet, AppRoute.Kyc)
    }

    private fun back() {
        router.pop()
    }

    private fun getInitialState(): TangemPayOnboardingScreenState {
        return TangemPayOnboardingScreenState(
            fullScreenLoading = true,
            buttonLoading = false,
            onGetCardClick = ::onGetCardClick,
            onBackClick = ::back,
        )
    }
}