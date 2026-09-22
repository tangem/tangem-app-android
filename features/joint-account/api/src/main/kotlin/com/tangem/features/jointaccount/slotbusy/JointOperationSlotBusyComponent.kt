package com.tangem.features.jointaccount.slotbusy

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface JointOperationSlotBusyComponent : ComposableBottomSheetComponent {

    interface Factory : ComponentFactory<Params, JointOperationSlotBusyComponent>

    data class Params(val networkName: String, val onDismiss: () -> Unit)
}