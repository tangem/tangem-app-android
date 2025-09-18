package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
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
        field = MutableStateFlow(TangemPayOnboardingScreenState())

    init {
        modelScope.launch {
            repository.validateDeeplink(params.deeplink)
                .onRight { isValid -> if (isValid) checkCustomerInfo() }
                .onLeft { exit() }
        }
    }

    private suspend fun checkCustomerInfo() {
        repository.getCustomerInfo()
            .onRight { customerInfo ->
                when {
                    !customerInfo.isKycApproved() -> {
                        screenState.value = screenState.value.copy(fullScreenLoading = false)
                    }
                    !customerInfo.isProductInstanceActive() -> {
                        // TODO [REDACTED_TASK_KEY]: create order and poll order status (API is not ready yet)
                    }
                    else -> exit()
                }
            }
            .onLeft { exit() }
    }

    private fun exit() {
        router.pop()
    }
}