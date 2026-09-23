package com.tangem.features.feed.crypto.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.features.feed.crypto.CryptoFeedEmptySearchComponent
import com.tangem.features.feed.crypto.model.search.CryptoFeedEmptySearchModel
import com.tangem.features.feed.crypto.ui.CryptoEmptySearchContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultCryptoFeedEmptySearchComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Unit,
) : CryptoFeedEmptySearchComponent, AppComponentContext by context {

    private val model: CryptoFeedEmptySearchModel = getOrCreateModel(params)

    private val listState = LazyListState()

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
        header: @Composable () -> Unit,
    ) {
        val state by model.uiState.collectAsStateWithLifecycle()

        val sheetState = bottomSheetState.value
        LaunchedEffect(sheetState) {
            model.containerBottomSheetState.value = sheetState
        }

        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }

        CryptoEmptySearchContent(
            state = state,
            listState = listState,
            contentPadding = contentPadding,
            modifier = modifier,
            header = header,
        )
    }

    @AssistedFactory
    interface Factory : CryptoFeedEmptySearchComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultCryptoFeedEmptySearchComponent
    }
}