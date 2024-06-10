package com.tangem.tap.features.customtoken.impl.presentation

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.tap.features.customtoken.impl.presentation.ui.AddCustomTokenScreen
import com.tangem.tap.features.customtoken.impl.presentation.viewmodels.AddCustomTokenViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Add custom token screen
 *
* [REDACTED_AUTHOR]
 */
@AndroidEntryPoint
internal class AddCustomTokenFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val viewModel = hiltViewModel<AddCustomTokenViewModel>().apply {
            LocalLifecycleOwner.current.lifecycle.addObserver(this)
        }
        val statusBarColor = TangemTheme.colors.background.secondary
        SystemBarsEffect {
            setSystemBarsColor(color = statusBarColor)
        }
        AddCustomTokenScreen(
            modifier = Modifier.systemBarsPadding(),
            stateHolder = viewModel.uiState,
        )
    }
}
