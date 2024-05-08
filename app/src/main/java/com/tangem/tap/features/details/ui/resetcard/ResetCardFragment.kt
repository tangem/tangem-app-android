package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class ResetCardFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    override lateinit var hapticManager: HapticManager

    private val viewModel = ResetCardViewModel(store)

    private var screenState: MutableState<ResetCardScreenState> =
        mutableStateOf(ResetCardScreenState.InitialState)

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        ResetCardScreen(
            state = screenState.value,
            onBackClick = { store.dispatch(NavigationAction.PopBackTo()) },
            modifier = modifier,
        )
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
