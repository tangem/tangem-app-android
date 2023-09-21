package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.transition.TransitionInflater
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import com.tangem.wallet.R
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class ResetCardFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    private val viewModel = ResetCardViewModel(store)

    private var screenState: MutableState<ResetCardScreenState> =
        mutableStateOf(viewModel.updateState(store.state.detailsState.cardSettingsState))

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        ResetCardScreen(
            modifier = modifier,
            state = screenState.value,
            onBackClick = { store.dispatch(NavigationAction.PopBackTo()) },
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