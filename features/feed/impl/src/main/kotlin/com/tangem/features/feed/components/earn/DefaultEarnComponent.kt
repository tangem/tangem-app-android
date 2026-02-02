package com.tangem.features.feed.components.earn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.features.feed.model.earn.EarnModel
import com.tangem.features.feed.ui.earn.EarnContent
import kotlinx.serialization.Serializable

internal class DefaultEarnComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val earnModel = getOrCreateModel<EarnModel, Params>(params = params)

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val background = LocalMainBottomSheetColor.current.value
        val state by earnModel.state.collectAsStateWithLifecycle()
        TangemTopAppBar(
            containerColor = background,
            title = stringResourceSafe(R.string.earn_title),
            startButton = TopAppBarButtonUM.Icon(
                iconRes = R.drawable.ic_back_24,
                onClicked = state.onBackClick,
                isEnabled = bottomSheetState.value == BottomSheetState.EXPANDED,
            ),
        )
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier) {
        val state by earnModel.state.collectAsStateWithLifecycle()
        EarnContent(
            state = state,
            modifier = modifier,
        )
    }

    @Serializable
    data class Params(
        val onBackClick: () -> Unit,
    )
}