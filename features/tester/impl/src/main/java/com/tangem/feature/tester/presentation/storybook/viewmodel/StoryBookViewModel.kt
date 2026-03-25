package com.tangem.feature.tester.presentation.storybook.viewmodel

import androidx.lifecycle.ViewModel
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.storybook.entity.StoryBookUM
import com.tangem.feature.tester.presentation.storybook.entity.StoryList
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class StoryBookViewModel @Inject constructor() : ViewModel() {

    private var router: InnerTesterRouter? = null

    private val _uiState = MutableStateFlow(
        StoryBookUM(
            onBackClick = ::onBackClick,
            onStoryClick = ::onStoryClick,
        ),
    )
    val uiState: StateFlow<StoryBookUM> = _uiState.asStateFlow()

    fun setupNavigation(router: InnerTesterRouter) {
        this.router = router
    }

    private fun onBackClick() {
        if (_uiState.value.currentPage !is StoryList) {
            _uiState.update { it.copy(currentPage = StoryList) }
        } else {
            router?.back()
        }
    }

    private fun onStoryClick(factory: StoryPageFactory) {
        _uiState.update { state ->
            state.copy(
                currentPage = factory.create { update ->
                    _uiState.update { s -> s.copy(currentPage = update(s.currentPage)) }
                },
            )
        }
    }
}