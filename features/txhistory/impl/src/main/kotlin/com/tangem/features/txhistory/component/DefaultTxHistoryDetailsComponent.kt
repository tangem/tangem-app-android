package com.tangem.features.txhistory.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.rating.RatingComponent
import com.tangem.features.txhistory.model.TxHistoryDetailsModel
import com.tangem.features.txhistory.ui.TxHistoryDetailsModalBottomSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTxHistoryDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: TxHistoryDetailsComponent.Params,
    ratingComponentFactory: RatingComponent.Factory,
) : TxHistoryDetailsComponent, AppComponentContext by context {

    private val model: TxHistoryDetailsModel = getOrCreateModel(params)

    private val ratingSlot = childSlot(
        key = RATING_SLOT_KEY,
        source = model.ratingSlotNavigation,
        serializer = null,
        childFactory = { ratingParams, ctx ->
            ratingComponentFactory.create(childByContext(ctx), ratingParams)
        },
    )

    init {
        model.activateRatingForSwap()
    }

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val ratingSlotState by ratingSlot.subscribeAsState()
        val ratingComponent = ratingSlotState.child?.instance

        TxHistoryDetailsModalBottomSheetContent(
            state = state,
            onDismiss = ::dismiss,
            ratingContent = ratingComponent?.let { component ->
                { component.Content(modifier = Modifier.fillMaxWidth()) }
            },
        )
    }

    @AssistedFactory
    interface Factory : TxHistoryDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TxHistoryDetailsComponent.Params,
        ): DefaultTxHistoryDetailsComponent
    }

    private companion object {
        const val RATING_SLOT_KEY = "ratingSlot"
    }
}