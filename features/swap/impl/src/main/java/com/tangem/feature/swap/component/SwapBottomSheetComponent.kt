package com.tangem.feature.swap.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.feature.swap.component.chooseprovider.ChooseProviderComponent
import com.tangem.feature.swap.model.SwapBottomSheetRoute
import com.tangem.feature.swap.models.states.ChooseProviderSheetSource
import com.tangem.feature.swap.ui.SwapBottomSheet
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.api.SelectApprovalTypeComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

/** One swap sheet showing [Params.route]; its card look follows [SwapBottomSheetRoute.isMaterial]. */
internal class SwapBottomSheetComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
    private val giveApprovalComponentFactory: GiveApprovalComponent.Factory,
    private val selectApprovalTypeComponentFactory: SelectApprovalTypeComponent.Factory,
) : ComposableBottomSheetComponent, AppComponentContext by context {

    private val content: ComposableModularContentComponent = createContent(child(CONTENT_KEY))

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        SwapBottomSheet(
            content = content,
            isMaterial = params.route.isMaterial,
            onBack = ::onBack,
            onDismiss = ::dismiss,
        )
    }

    // Approval screens own their cancel semantics, so back on them goes through their own dismiss.
    private fun onBack() {
        val approval = content as? ComposableBottomSheetComponent
        if (approval != null) approval.dismiss() else dismiss()
    }

    private fun createContent(contentContext: AppComponentContext): ComposableModularContentComponent =
        when (val route = params.route) {
            SwapBottomSheetRoute.ChooseProvider -> ChooseProviderComponent(
                context = contentContext,
                params = ChooseProviderComponent.Params(
                    source = params.providersSheetSource,
                    onCloseClick = ::dismiss,
                ),
            )
            is SwapBottomSheetRoute.SelectApprovalType -> selectApprovalTypeComponentFactory.create(
                context = contentContext,
                params = route.params,
            )
            is SwapBottomSheetRoute.GiveApproval -> giveApprovalComponentFactory.create(
                context = contentContext,
                params = route.params,
            )
        }

    data class Params(
        val route: SwapBottomSheetRoute,
        val providersSheetSource: StateFlow<ChooseProviderSheetSource>,
        val onDismiss: () -> Unit,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): SwapBottomSheetComponent
    }

    private companion object {
        const val CONTENT_KEY = "swapBottomSheetContent"
    }
}