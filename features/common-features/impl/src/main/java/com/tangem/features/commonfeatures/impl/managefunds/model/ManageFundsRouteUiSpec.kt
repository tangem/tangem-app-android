package com.tangem.features.commonfeatures.impl.managefunds.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.commonfeatures.api.managefunds.ManageFundsComponent
import com.tangem.features.commonfeatures.impl.R

internal data class ManageFundsRouteUiSpec(
    val title: TextReference,
    val shouldApplyHorizontalPadding: Boolean,
    val shouldFillHeight: Boolean,
)

internal fun ManageFundsModel.UiRoute.uiSpec(flowType: ManageFundsComponent.FlowType): ManageFundsRouteUiSpec {
    val isTransfer = flowType == ManageFundsComponent.FlowType.Transfer
    return when (this) {
        ManageFundsModel.UiRoute.Loading -> ManageFundsRouteUiSpec(
            title = resourceReference(if (isTransfer) R.string.common_choose_token else R.string.common_add_funds),
            shouldApplyHorizontalPadding = false,
            shouldFillHeight = false,
        )
        ManageFundsModel.UiRoute.ChooseToken -> ManageFundsRouteUiSpec(
            title = resourceReference(R.string.common_choose_token),
            shouldApplyHorizontalPadding = false,
            shouldFillHeight = true,
        )
        ManageFundsModel.UiRoute.UserPortfolio -> ManageFundsRouteUiSpec(
            title = resourceReference(R.string.common_add_funds),
            shouldApplyHorizontalPadding = false,
            shouldFillHeight = false,
        )
        ManageFundsModel.UiRoute.TokenActions -> ManageFundsRouteUiSpec(
            title = resourceReference(if (isTransfer) R.string.common_transfer else R.string.common_get_token),
            shouldApplyHorizontalPadding = true,
            shouldFillHeight = true,
        )
    }
}