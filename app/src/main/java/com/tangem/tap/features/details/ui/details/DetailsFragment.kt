package com.tangem.tap.features.details.ui.details

import androidx.compose.runtime.Composable
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
class DetailsFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    private val detailsViewModel = DetailsViewModel(store)

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        DetailsScreen(
            state = detailsViewModel.detailsScreenState.value,
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
        detailsViewModel.detailsScreenState.value = detailsViewModel.updateState(state)
    }
}
