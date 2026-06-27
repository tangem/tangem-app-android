package com.tangem.features.foryou.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.foryou.ForYouComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultForYouComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Suppress("UnusedPrivateMember") @Assisted params: Unit,
) : AppComponentContext by context, ForYouComponent {

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        TODO("Not yet implemented")
    }

    @AssistedFactory
    interface Factory : ForYouComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultForYouComponent
    }
}