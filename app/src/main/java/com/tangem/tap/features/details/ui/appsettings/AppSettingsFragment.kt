package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class AppSettingsFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    lateinit var appCurrencyRepository: AppCurrencyRepository

    private val viewModel by lazy(mode = LazyThreadSafetyMode.NONE) {
        AppSettingsViewModel(store, appCurrencyRepository)
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        AppSettingsScreen(
            modifier = modifier,
            state = viewModel.uiState,
            onBackClick = {
                store.dispatch(DetailsAction.ResetCardSettingsData)
                store.dispatch(NavigationAction.PopBackTo())
            },
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkBiometricsStatus(lifecycleScope)
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
        viewModel.updateState(state)
    }
}