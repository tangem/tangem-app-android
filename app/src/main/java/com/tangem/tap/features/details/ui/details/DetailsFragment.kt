package com.tangem.tap.features.details.ui.details

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class DetailsFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var walletsRepository: WalletsRepository

    @Inject
    lateinit var userWalletsListManager: UserWalletsListManager

    private lateinit var detailsViewModel: DetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detailsViewModel = DetailsViewModel(
            store = store,
            walletsRepository = walletsRepository,
            userWalletsListManager = userWalletsListManager,
        )
        Analytics.send(Settings.ScreenOpened())
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        DetailsScreen(
            modifier = modifier,
            state = detailsViewModel.detailsScreenState.value,
            onBackClick = { store.dispatchNavigationAction(AppRouter::pop) },
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
        detailsViewModel.detailsScreenState.value = detailsViewModel.updateState(state)
    }
}