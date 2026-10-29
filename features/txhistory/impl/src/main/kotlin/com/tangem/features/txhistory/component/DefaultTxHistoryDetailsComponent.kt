package com.tangem.features.txhistory.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.rating.RatingComponent
import com.tangem.features.txhistory.joint.operation.route.JointOperationRoutes
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

    /** The details card and, for a joint operation, its signers; the children are just the routes the sheet draws */
    private val contentStack = childStack(
        key = CONTENT_STACK_KEY,
        source = model.stackNavigation,
        serializer = JointOperationRoutes.serializer(),
        initialConfiguration = JointOperationRoutes.Operation,
        handleBackButton = false,
        childFactory = { route, _ -> route },
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
        val stack by contentStack.subscribeAsState()
        val signers by model.signersState.collectAsStateWithLifecycle()

        TxHistoryDetailsModalBottomSheetContent(
            state = state,
            onDismiss = ::dismiss,
            route = stack.active.configuration,
            signers = signers,
            onBack = ::onBack,
            ratingContent = ratingComponent?.let { component ->
                { modifier -> component.Content(modifier = modifier) }
            },
        )
    }

    private fun onBack() {
        when (contentStack.value.active.configuration) {
            JointOperationRoutes.Signers -> model.stackNavigation.pop()
            JointOperationRoutes.Operation -> dismiss()
        }
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
        const val CONTENT_STACK_KEY = "contentStack"
    }
}