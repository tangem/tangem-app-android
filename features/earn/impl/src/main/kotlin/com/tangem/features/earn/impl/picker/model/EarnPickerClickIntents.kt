package com.tangem.features.earn.impl.picker.model

import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnRowInfo
import com.tangem.features.earn.product.api.product.EarnRowKind
import com.tangem.features.earn.product.api.session.EarnAction

internal interface EarnPickerClickIntents {

    fun onProductClick(id: EarnProductId)

    /** The converter passes the product the user saw, so a tap acts on it even if the selection has moved on. */
    fun onActionClick(product: EarnProduct, action: EarnAction)

    fun onBlockedClick(block: EarnBlock)

    fun onAddFundsClick()

    fun onRetryClick(product: EarnProduct)

    fun onOverviewRetryClick()

    fun onIncentiveClick()

    fun onTabClick(tab: EarnPickerTab)

    fun onInfoClick(info: EarnRowInfo, row: EarnRowKind?)

    fun onLinkClick(url: String, row: EarnRowKind?)

    fun onCloseClick()
}