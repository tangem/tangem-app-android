package com.tangem.features.tangempay.tiers.select

internal interface TangemPaySelectPlanClickIntents {

    fun onPlanSelected(index: Int)

    fun onSelectClick()

    fun onComparePlansClick()

    fun onCompareDismiss()

    fun onBackClick()

    fun onCloseClick()

    fun onConfirmClick()

    fun onRetryClick()

    fun onLinkClick(url: String)
}