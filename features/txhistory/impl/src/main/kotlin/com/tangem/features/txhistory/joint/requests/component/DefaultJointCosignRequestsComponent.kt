package com.tangem.features.txhistory.joint.requests.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.txhistory.component.JointCosignRequestsComponent
import com.tangem.features.txhistory.entity.JointCosignRequestsUM
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.requests.JointOperationSlotConfig
import com.tangem.features.txhistory.joint.requests.model.JointCosignRequestsModel
import com.tangem.features.txhistory.joint.requests.ui.jointCosignRequestItems
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

internal class DefaultJointCosignRequestsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointCosignRequestsComponent.Params,
    private val jointOperationComponentFactory: JointOperationComponent.Factory,
) : JointCosignRequestsComponent, AppComponentContext by appComponentContext {

    private val model: JointCosignRequestsModel = getOrCreateModel(params)

    private val operationSlot = childSlot(
        key = OPERATION_SLOT_KEY,
        source = model.operationNavigation,
        serializer = JointOperationSlotConfig.serializer(),
        handleBackButton = true,
        childFactory = { config, ctx ->
            jointOperationComponentFactory.create(
                context = childByContext(ctx),
                params = JointOperationComponent.Params(
                    userWalletId = params.userWalletId,
                    currency = params.currency,
                    operationId = config.operationId,
                    onDismiss = model.operationNavigation::dismiss,
                ),
            )
        },
    )

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
        // The slot above must already be subscribed to model.operationNavigation before this runs,
        // otherwise a pending operation's activation has no observer and is silently dropped.
        model.consumePendingOperation()
    }

    override val state: StateFlow<JointCosignRequestsUM>
        get() = model.state

    override fun LazyListScope.jointCosignRequestsContent(state: JointCosignRequestsUM) {
        jointCosignRequestItems(state = state)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val operation by operationSlot.subscribeAsState()
        operation.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : JointCosignRequestsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointCosignRequestsComponent.Params,
        ): DefaultJointCosignRequestsComponent
    }

    private companion object {
        const val OPERATION_SLOT_KEY = "jointOperationSlot"
    }
}