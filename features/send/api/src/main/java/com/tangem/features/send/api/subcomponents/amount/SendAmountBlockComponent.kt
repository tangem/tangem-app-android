package com.tangem.features.send.api.subcomponents.amount

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent

interface SendAmountBlockComponent : ComposableContentComponent {

    fun updateState(amountUM: AmountState)

    interface Factory {
        fun create(
            context: AppComponentContext,
            params: SendAmountComponentParams.AmountBlockParams,
            onClick: () -> Unit,
            onResult: (AmountState) -> Unit,
        ): SendAmountBlockComponent
    }
}