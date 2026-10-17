package com.tangem.features.tangempay.hotwallet

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.model.TangemPayOnboardingStarter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class TangemPayHotWalletOnboardingModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val onboardingStarter: TangemPayOnboardingStarter,
) : Model() {

    val uiState: StateFlow<TangemPayHotWalletOnboardingUM>
        field = MutableStateFlow(
            TangemPayHotWalletOnboardingUM(
                isLoading = false,
                onGetCardClick = ::onGetCardClick,
                onTermsClick = ::onTermsClick,
                onTermsOfServiceClick = ::onTermsOfServiceClick,
                onPrivacyPolicyClick = ::onPrivacyPolicyClick,
            ),
        )

    private fun onTermsClick() {
        urlOpener.openUrl(TangemPayConstants.termsAndLimitsLink())
    }

    private fun onTermsOfServiceClick() {
        urlOpener.openUrl(TERMS_OF_SERVICE_URL)
    }

    private fun onPrivacyPolicyClick() {
        urlOpener.openUrl(PRIVACY_POLICY_URL)
    }

    private fun onGetCardClick() {
        TangemLogger.i("[TangemPay][HWO]onGetCardClick")
        setLoading(isLoading = true)

        modelScope.launch {
            val userWallet = onboardingStarter.createHotWallet(onLoadingChange = ::setLoading) ?: return@launch

            router.replaceAll(
                AppRoute.TangemPayOnboarding(
                    mode = AppRoute.TangemPayOnboarding.Mode.FirstSetup(userWallet.walletId),
                ),
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        uiState.update { state -> state.copy(isLoading = isLoading) }
    }

    private companion object {
        const val TERMS_OF_SERVICE_URL = "https://www.paera.com/tangempay"
        const val PRIVACY_POLICY_URL = "https://tangem.com/docs/en/pay-privacy-policy.pdf"
    }
}