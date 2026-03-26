package com.tangem.features.virtualaccount.onboarding.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.promo.ShouldShowStoriesUseCase
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.features.virtualaccount.onboarding.impl.ui.state.VirtualAccountOnboardingUiState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class VirtualAccountOnboardingModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: AppRouter,
    private val shouldShowStoriesUseCase: ShouldShowStoriesUseCase,
) : Model() {

    val uiState: StateFlow<VirtualAccountOnboardingUiState>
        field = MutableStateFlow(VirtualAccountOnboardingUiState())

    init {
        modelScope.launch {
            val storyId = StoryContentIds.STORY_FIRST_TIME_SWAP.id
            if (shouldShowStoriesUseCase.invokeSync(storyId)) {
                router.push(
                    AppRoute.Stories(
                        storyId = storyId,
                        nextScreen = null,
                        screenSource = SCREEN_SOURCE,
                    ),
                )
            }
        }
    }

    fun onBackClick() {
        router.pop()
    }

    private companion object {
        const val SCREEN_SOURCE = "VirtualAccountOnboarding"
    }
}