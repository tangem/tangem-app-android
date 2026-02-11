package com.tangem.features.feed.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.feed.model.market.list.analytics.MarketsListAnalyticsEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeedEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)

    init {
        observeContainerBottomSheet()
    }

    private fun observeContainerBottomSheet() {
        containerBottomSheetState.onEach { bottomSheetState ->
            if (bottomSheetState == BottomSheetState.EXPANDED) {
                analyticsEventHandler.send(MarketsListAnalyticsEvent.BottomSheetOpened())
            }
        }.launchIn(modelScope)
    }
}