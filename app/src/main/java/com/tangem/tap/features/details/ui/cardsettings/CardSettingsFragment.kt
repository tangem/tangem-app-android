package com.tangem.tap.features.details.ui.cardsettings

import androidx.activity.compose.BackHandler
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
internal class CardSettingsFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val viewModel: CardSettingsViewModel by viewModels()

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        BackHandler { store.dispatchNavigationAction(AppRouter::pop) }

        val state by viewModel.screenState.collectAsStateWithLifecycle()

        CardSettingsScreen(modifier = modifier, state = state)
    }
}