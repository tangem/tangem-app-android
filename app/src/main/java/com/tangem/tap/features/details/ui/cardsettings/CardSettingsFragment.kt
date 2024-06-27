package com.tangem.tap.features.details.ui.cardsettings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.viewModels
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.tap.features.details.redux.DetailsAction
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
        LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

        CardSettingsScreen(
            modifier = modifier,
            state = viewModel.screenState.value,
            onBackClick = {
                store.dispatch(DetailsAction.ResetCardSettingsData)
                store.dispatch(NavigationAction.PopBackTo())
            },
        )
    }
}