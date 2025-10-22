package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.ui.TangemPayOnboardingScreenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val repository: OnboardingRepository,
) : Model() {

    private val params = paramsContainer.require<TangemPayOnboardingComponent.Params>()

    val screenState: StateFlow<TangemPayOnboardingScreenState>
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

    private fun openKyc() {
        router.replaceAll(AppRoute.Wallet, AppRoute.Kyc)
    }

    private fun back() {
        router.pop()
    }

    private fun showOnboarding() {
        screenState.update {
            it.copy(fullScreenLoading = false)
        }
    }

    private suspend fun checkCustomerInfo() {
        repository.getCustomerInfo()
            .onRight { customerInfo ->
                when {
                    !customerInfo.isKycApproved -> {
                        when (params) {
                            is TangemPayOnboardingComponent.Params.Deeplink ->
                                screenState.value = screenState.value.copy(fullScreenLoading = false)
                            else -> openKyc()
                        }
                    }
                    else -> back()
                }
            }
            .onLeft { back() }
    }

    private fun getInitialState(): TangemPayOnboardingScreenState {
        return TangemPayOnboardingScreenState(
            fullScreenLoading = true,
            buttonLoading = false,
            onGetCardClick = ::openKyc,
            onBackClick = ::back,
        )
    }
}