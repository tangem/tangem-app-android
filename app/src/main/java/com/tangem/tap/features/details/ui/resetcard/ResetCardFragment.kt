package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ResetCardFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val viewModel: ResetCardViewModel by viewModels()

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state by viewModel.screenState.collectAsStateWithLifecycle()

        ResetCardScreen(
            state = state,
            onBackClick = { store.dispatchNavigationAction(AppRouter::pop) },
            modifier = modifier,
        )
    }
}
