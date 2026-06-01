package com.tangem.features.commonfeatures.impl.addfunds.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.commonfeatures.impl.R
import com.tangem.core.ui.R as CoreR

internal data class AddFundsRouteUiSpec(
    val title: TextReference,
    val shouldApplyHorizontalPadding: Boolean,
    val shouldFillHeight: Boolean,
)

internal fun AddFundsModel.UiRoute.uiSpec(): AddFundsRouteUiSpec = when (this) {
    AddFundsModel.UiRoute.Loading -> AddFundsRouteUiSpec(
        title = resourceReference(R.string.common_add_funds),
        shouldApplyHorizontalPadding = false,
        shouldFillHeight = false,
    )
    AddFundsModel.UiRoute.ChooseToken -> AddFundsRouteUiSpec(
        title = resourceReference(R.string.common_add_funds),
        shouldApplyHorizontalPadding = false,
        shouldFillHeight = true,
    )
    AddFundsModel.UiRoute.UserPortfolio -> AddFundsRouteUiSpec(
        title = resourceReference(R.string.common_add_funds),
        shouldApplyHorizontalPadding = false,
        shouldFillHeight = false,
    )
    AddFundsModel.UiRoute.TokenActions -> AddFundsRouteUiSpec(
        title = resourceReference(CoreR.string.common_get_token),
        shouldApplyHorizontalPadding = true,
        shouldFillHeight = true,
    )
}