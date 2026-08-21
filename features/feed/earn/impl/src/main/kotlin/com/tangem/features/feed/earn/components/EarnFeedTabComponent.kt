package com.tangem.features.feed.earn.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.feed.nav.FeedTabComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class EarnFeedTabComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Suppress("UnusedPrivateProperty") @Assisted params: Unit,
) : FeedTabComponent, AppComponentContext by context {

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, contentPadding: PaddingValues, modifier: Modifier) {
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Unit): EarnFeedTabComponent
    }
}