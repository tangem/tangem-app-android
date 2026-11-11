package com.tangem.features.earn.impl.sheet.model

import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.session.EarnAction

internal interface EarnYieldSheetClickIntents {

    fun onActionClick(productId: EarnProductId, action: EarnAction)

    fun onDismissClick()
}