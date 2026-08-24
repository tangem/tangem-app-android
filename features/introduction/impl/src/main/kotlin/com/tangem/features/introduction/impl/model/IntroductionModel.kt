package com.tangem.features.introduction.impl.model

import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class IntroductionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<IntroductionComponent.Params>()

    val uiState: StateFlow<IntroductionUM>
        field = MutableStateFlow(
            IntroductionUM(
                // TODO [REDACTED_TASK_KEY]: open the Create wallet and I have a wallet bottom sheets
                onCreateWalletClick = {},
                onIHaveWalletClick = {},
                onTermsOfServiceClick = ::onTermsOfServiceClick,
                onPrivacyPolicyClick = ::onPrivacyPolicyClick,
            ),
        )

    init {
        analyticsEventHandler.send(IntroductionProcess.ScreenOpened())

        when (params.launchMode) {
            // TODO [REDACTED_TASK_KEY]: an NFC tap must start the card scan; the scan entry point arrives with the
            //  Create wallet bottom sheet.
            InitScreenLaunchMode.WithCardScan,
            InitScreenLaunchMode.Standard,
            -> Unit
        }
    }

    private fun onTermsOfServiceClick() {
        urlOpener.openUrl(TERMS_OF_SERVICE_URL)
    }

    private fun onPrivacyPolicyClick() {
        urlOpener.openUrl(PRIVACY_POLICY_URL)
    }

    private companion object {
        const val TERMS_OF_SERVICE_URL = "https://tangem.com/terms-of-service/"
        const val PRIVACY_POLICY_URL = "https://tangem.com/privacy-policy/"
    }
}