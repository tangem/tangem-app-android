package com.tangem.features.collectibles.impl.stories.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.collectibles.impl.stories.model.converter.CollectiblesStoriesUMConverter
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStoriesUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class CollectiblesStoriesModel @Inject constructor(
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val uiState: StateFlow<CollectiblesStoriesUM>
        field = MutableStateFlow(
            CollectiblesStoriesUMConverter().convert(
                value = CollectiblesStoriesUMConverter.Callbacks(
                    onNextSlideClick = ::onNextSlideClick,
                    onPreviousSlideClick = ::onPreviousSlideClick,
                    onContinueClick = ::onContinueClick,
                    onCloseClick = ::onCloseClick,
                ),
            ),
        )

    private fun onNextSlideClick() {
        uiState.update { state ->
            if (state.currentIndex == state.slides.lastIndex) {
                state
            } else {
                state.copy(currentIndex = state.currentIndex + 1)
            }
        }
    }

    private fun onPreviousSlideClick() {
        uiState.update { state ->
            if (state.currentIndex == 0) {
                state
            } else {
                state.copy(currentIndex = state.currentIndex - 1)
            }
        }
    }

    private fun onContinueClick() {
        val state = uiState.value

        if (state.currentIndex == state.slides.lastIndex) {
            router.pop()
        } else {
            onNextSlideClick()
        }
    }

    private fun onCloseClick() {
        router.pop()
    }
}