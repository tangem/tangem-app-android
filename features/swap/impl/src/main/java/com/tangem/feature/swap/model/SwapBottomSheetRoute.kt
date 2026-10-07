package com.tangem.feature.swap.model

import com.tangem.feature.swap.component.slippage.SwapSlippageComponent
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.api.SelectApprovalTypeComponent

/**
 * A swap sheet. [isMaterial] picks both the card look and the slot the sheet opens in: glass sheets get their own
 * slot above the flat one, because switching the card material inside one animating modal janks.
 */
internal sealed interface SwapBottomSheetRoute {

    val isMaterial: Boolean

    data object ChooseProvider : SwapBottomSheetRoute {
        override val isMaterial: Boolean = false
    }

    data class SelectApprovalType(val params: SelectApprovalTypeComponent.Params) : SwapBottomSheetRoute {
        override val isMaterial: Boolean = true
    }

    data class GiveApproval(val params: GiveApprovalComponent.Params) : SwapBottomSheetRoute {
        override val isMaterial: Boolean = true
    }

    data class Slippage(val params: SwapSlippageComponent.Params) : SwapBottomSheetRoute {
        override val isMaterial: Boolean = true
    }
}