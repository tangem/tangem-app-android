package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class AppSettingsFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var appCurrencyRepository: AppCurrencyRepository

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val viewModel = hiltViewModel<AppSettingsViewModel>().apply {
            LocalLifecycleOwner.current.lifecycle.addObserver(observer = this)
        }
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        AppSettingsScreen(
            modifier = modifier,
            state = state,
            onBackClick = {
                store.dispatch(DetailsAction.ResetCardSettingsData)
                store.dispatch(NavigationAction.PopBackTo())
            },
        )
    }
}
