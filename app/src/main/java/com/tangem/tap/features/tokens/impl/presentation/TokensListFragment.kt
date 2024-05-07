package com.tangem.tap.features.tokens.impl.presentation

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.features.tokens.impl.presentation.ui.TokensListScreen
import com.tangem.tap.features.tokens.impl.presentation.viewmodels.TokensListViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment with list of tokens
 *
[REDACTED_AUTHOR]
 */
@AndroidEntryPoint
internal class TokensListFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    override lateinit var hapticManager: HapticManager

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val viewModel = hiltViewModel<TokensListViewModel>().apply {
            LocalLifecycleOwner.current.lifecycle.addObserver(this)
        }
        val statusBarColor = TangemTheme.colors.background.secondary
        SystemBarsEffect {
            setSystemBarsColor(color = statusBarColor)
        }
        TokensListScreen(
            modifier = Modifier.systemBarsPadding(),
            stateHolder = viewModel.uiState,
        )
    }
}