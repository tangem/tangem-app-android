package com.tangem.tap.features.details.ui.cardsettings.coderecovery

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
class AccessCodeRecoveryFragment : ComposeFragment() {

    private val viewModel: AccessCodeRecoveryViewModel by viewModels()

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state by viewModel.screenState.collectAsStateWithLifecycle()

        AccessCodeRecoveryScreen(
            state = state,
            onBackClick = { store.dispatchNavigationAction(AppRouter::pop) },
        )
    }
}