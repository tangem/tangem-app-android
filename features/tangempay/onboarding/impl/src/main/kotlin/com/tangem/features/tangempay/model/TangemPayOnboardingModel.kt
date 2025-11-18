package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.model.transformers.TangemPayOnboardingButtonLoadingTransformer
import com.tangem.features.tangempay.ui.TangemPayOnboardingScreenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import com.tangem.utils.transformer.update as transformerUpdate
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
    private val urlOpener: UrlOpener,
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
        uiState.update {
            TangemPayOnboardingScreenState.Content(
                onBack = it.onBack,
                onTermsClick = ::onTermsClick,
                buttonConfig = TangemPayOnboardingScreenState.Content.ButtonConfig(
                    isLoading = false,
                    onClick = ::onGetCardClick,
                ),
            )
        }
    }

    private suspend fun checkCustomerInfo() {
        repository.getCustomerInfo()
            .onRight { customerInfo ->
                when {
                    !customerInfo.isKycApproved -> {
                        when (params) {
                            is TangemPayOnboardingComponent.Params.Deeplink -> showOnboarding()
                            else -> openKyc()
                        }
                    }
                    else -> back()
                }
            }
            .onLeft { back() }
    }

    private fun onTermsClick() {
        urlOpener.openUrl(TangemPayConstants.TERMS_AND_LIMITS_LINK)
    }

    private fun onGetCardClick() {
        uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = true))
        modelScope.launch {
            val result = produceInitialDataUseCase()
            if (result.isLeft()) {
                Timber.e("Error producing initial data: ${result.leftOrNull()?.message}")
                uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = false))
                return@launch
            }

            repository.getCustomerInfo()
                .fold(
                    ifLeft = {
                        Timber.e("Error getCustomerInfo: ${it.errorCode}")
                        uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = false))
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
        return TangemPayOnboardingScreenState.Loading(onBack = ::back)
    }
}