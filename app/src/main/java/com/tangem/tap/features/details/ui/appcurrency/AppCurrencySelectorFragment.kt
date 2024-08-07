package com.tangem.tap.features.details.ui.appcurrency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.screen.ComposeFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class AppCurrencySelectorFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val viewModel: AppCurrencySelectorViewModel by viewModels()

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        NavigationBar3ButtonsScrim()
        AppCurrencySelectorScreen(
            modifier = modifier,
            state = uiState,
        )
    }
}