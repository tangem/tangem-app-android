package com.tangem.tap.features.details.ui.details

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.navigation.email.EmailSender
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.domain.feedback.FeedbackManagerFeatureToggles
import com.tangem.domain.feedback.GetSupportFeedbackEmailUseCase
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.features.details.redux.DetailsState
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class DetailsFragment : ComposeFragment(), StoreSubscriber<DetailsState> {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    lateinit var walletsRepository: WalletsRepository

    @Inject
    lateinit var feedbackManagerFeatureToggles: FeedbackManagerFeatureToggles

    @Inject
    lateinit var getSupportFeedbackEmailUseCase: GetSupportFeedbackEmailUseCase

    @Inject
    lateinit var emailSender: EmailSender

    private lateinit var detailsViewModel: DetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detailsViewModel = DetailsViewModel(
            store = store,
            walletsRepository = walletsRepository,
            feedbackManagerFeatureToggles = feedbackManagerFeatureToggles,
            getSupportFeedbackEmailUseCase = getSupportFeedbackEmailUseCase,
            emailSender = emailSender,
        )
        Analytics.send(Settings.ScreenOpened())
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        DetailsScreen(
            modifier = modifier,
            state = detailsViewModel.detailsScreenState.value,
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
        detailsViewModel.detailsScreenState.value = detailsViewModel.updateState(state)
    }
}
