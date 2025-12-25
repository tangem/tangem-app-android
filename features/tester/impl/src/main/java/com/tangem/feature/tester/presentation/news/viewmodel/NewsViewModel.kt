package com.tangem.feature.tester.presentation.news.viewmodel

import androidx.lifecycle.ViewModel
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.navigation.TesterScreen
import com.tangem.feature.tester.presentation.news.state.NewsUM
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
internal class NewsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<NewsUM> = _uiState

    private var router: InnerTesterRouter? = null

    fun setupNavigation(router: InnerTesterRouter) {
        this.router = router
    }

    private fun createInitialState(): NewsUM {
        return NewsUM(
            onBackClick = ::onBackClick,
            buttons = persistentSetOf(
                NewsUM.ButtonUM.NEWS_DETAILS,
                NewsUM.ButtonUM.NEWS_DETAILS_BOTTOM_SHEET,
            ),
            onButtonClick = ::onButtonClick,
        )
    }

    private fun onBackClick() {
        router?.back()
    }

    private fun onButtonClick(button: NewsUM.ButtonUM) {
        when (button) {
            NewsUM.ButtonUM.NEWS_DETAILS -> router?.open(TesterScreen.NEWS_DETAILS)
            NewsUM.ButtonUM.NEWS_DETAILS_BOTTOM_SHEET -> router?.open(TesterScreen.NEWS_DETAILS_BOTTOM_SHEET)
        }
    }
}