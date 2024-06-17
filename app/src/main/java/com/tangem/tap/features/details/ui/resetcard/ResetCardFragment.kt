package com.tangem.tap.features.details.ui.resetcard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class ResetCardFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val viewModel: ResetCardViewModel by viewModels()

    private var screenState = MutableStateFlow<ResetCardScreenState>(ResetCardScreenState.InitialState)

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state = screenState.collectAsStateWithLifecycle().value

        ResetCardScreen(
            state = state,
            onBackClick = { store.dispatchNavigationAction(AppRouter::pop) },
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