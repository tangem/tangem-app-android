package com.tangem.features.collectibles.impl.onboarding.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.onboarding.model.converter.CollectiblesOnboardingUMConverter
import com.tangem.features.collectibles.impl.onboarding.ui.state.CollectiblesOnboardingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Model of the Collectibles Onboarding screen.
 *
 * Account creation is not wired yet: the CTA goes straight to the stories.
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

    init {
        preloadStory()
    }

    /** Warms the story up while the user reads the onboarding. */
    @Suppress("EmptyFunctionBlock")
    private fun preloadStory() {
        // TODO([REDACTED_TASK_KEY]): nothing to warm while the slides are local — see the TODO in
        //  CollectiblesStoriesUMConverter. With a backend story, fetch it here with `refresh = true` and preload
        //  its images, the way YieldBoostStoryPreloader does.
    }

    /** Whether the stories screen has something to show. */
    @Suppress("FunctionOnlyReturningConstant")
    private fun isStoryReady(): Boolean {
        // TODO([REDACTED_TASK_KEY]): local slides are always there. With a backend story this asks
        //  ShouldShowStoriesInteractor / GetStoryContentUseCase, gets slow enough to need `isCreatingAccount`
        //  raised around the call, and starts returning false — the case onCreateAccountClick already handles.
        return true
    }

    private fun onCloseClick() {
        router.pop()
    }

    private fun onCreateAccountClick() {
        if (isStoryReady()) {
            router.replaceAll(CollectiblesRoute.Main, CollectiblesRoute.Stories)
        } else {
            router.replaceAll(CollectiblesRoute.Main)
        }
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