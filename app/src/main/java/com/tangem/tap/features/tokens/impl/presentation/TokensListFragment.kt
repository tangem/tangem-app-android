package com.tangem.tap.features.tokens.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.tap.features.tokens.impl.presentation.ui.TokensListScreen
import com.tangem.tap.features.tokens.impl.presentation.viewmodels.TokensListViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment with list of tokens
 *
* [REDACTED_AUTHOR]
 */
@AndroidEntryPoint
internal class TokensListFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val viewModel = hiltViewModel<TokensListViewModel>().apply {
            LocalLifecycleOwner.current.lifecycle.addObserver(this)
        }
        TokensListScreen(
            stateHolder = viewModel.uiState,
        )
    }
}
