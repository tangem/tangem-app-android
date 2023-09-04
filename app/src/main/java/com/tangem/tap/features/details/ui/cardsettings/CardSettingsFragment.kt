package com.tangem.tap.features.details.ui.cardsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.transition.TransitionInflater
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import com.tangem.wallet.R
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class CardSettingsFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    private val viewModel = CardSettingsViewModel(store)

    private var screenState: MutableState<CardSettingsScreenState> =
        mutableStateOf(viewModel.updateState(store.state.detailsState.cardSettingsState))

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        CardSettingsScreen(
            modifier = modifier,
            state = screenState.value,
            onBackClick = {
                store.dispatch(DetailsAction.ResetCardSettingsData)
                store.dispatch(NavigationAction.PopBackTo())
            },
        )
    }

    override fun TransitionInflater.inflateTransitions(): Boolean {
        enterTransition = inflateTransition(R.transition.fade)
        exitTransition = inflateTransition(R.transition.fade)

        return true
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.detailsState == newState.detailsState
            }.select { it.detailsState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: DetailsState) {
        if (activity == null || view == null) return
        screenState.value = viewModel.updateState(state.cardSettingsState)
    }
}