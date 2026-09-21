package com.tangem.features.jointaccount.slotbusy.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.jointaccount.slotbusy.JointOperationSlotBusyComponent
import com.tangem.features.jointaccount.slotbusy.ui.JointOperationSlotBusyBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointOperationSlotBusyComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointOperationSlotBusyComponent.Params,
) : JointOperationSlotBusyComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        JointOperationSlotBusyBottomSheet(networkName = params.networkName, onDismiss = params.onDismiss)
    }

    @AssistedFactory
    interface Factory : JointOperationSlotBusyComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointOperationSlotBusyComponent.Params,
        ): DefaultJointOperationSlotBusyComponent
    }
}