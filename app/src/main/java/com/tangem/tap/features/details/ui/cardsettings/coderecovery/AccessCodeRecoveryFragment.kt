package com.tangem.tap.features.details.ui.cardsettings.coderecovery

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
class AccessCodeRecoveryFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    private val viewModel = AccessCodeRecoveryViewModel(store)

    private var screenState: MutableState<AccessCodeRecoveryScreenState> =
        mutableStateOf(viewModel.updateState(store.state.detailsState.cardSettingsState?.accessCodeRecovery))

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    override lateinit var hapticManager: HapticManager

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        AccessCodeRecoveryScreen(
            state = screenState.value,
            onBackClick = { store.dispatch(NavigationAction.PopBackTo()) },
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
        screenState.value =
            viewModel.updateState(store.state.detailsState.cardSettingsState?.accessCodeRecovery)
    }
}
