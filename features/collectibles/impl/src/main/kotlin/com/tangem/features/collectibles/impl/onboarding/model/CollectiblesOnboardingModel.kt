package com.tangem.features.collectibles.impl.onboarding.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.collectibles.impl.onboarding.model.converter.CollectiblesOnboardingUMConverter
import com.tangem.features.collectibles.impl.onboarding.ui.state.CollectiblesOnboardingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Model of the Collectibles Onboarding screen.
 *
 * Account creation is not wired yet: the CTA has no domain behind it, so pressing it does nothing rather than
 * raising a loader that would never resolve.
 */
@ModelScoped
internal class CollectiblesOnboardingModel @Inject constructor(
    private val router: Router,
    private val urlOpener: UrlOpener,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val uiState: StateFlow<CollectiblesOnboardingUM>
        field = MutableStateFlow(
            CollectiblesOnboardingUMConverter().convert(
                value = CollectiblesOnboardingUMConverter.Callbacks(
                    onCloseClick = ::onCloseClick,
                    onCreateAccountClick = ::onCreateAccountClick,
                    onCollectiblesTermsClick = ::onCollectiblesTermsClick,
                    onTangemTermsClick = ::onTangemTermsClick,
                ),
            ),
        )

    private fun onCloseClick() {
        router.pop()
    }

    private fun onCreateAccountClick() {
        // No-op: account creation has no domain behind it yet.
    }

    private fun onCollectiblesTermsClick() {
        // No-op: the service has not supplied its terms URL yet.
    }

    private fun onTangemTermsClick() {
        urlOpener.openUrl(TANGEM_TERMS_URL)
    }

    private companion object {

        const val TANGEM_TERMS_URL = "https://tangem.com/tangem_tos.html"
    }
}