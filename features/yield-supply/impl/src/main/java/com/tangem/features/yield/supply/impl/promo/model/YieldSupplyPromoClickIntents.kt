package com.tangem.features.yield.supply.impl.promo.model

internal interface YieldSupplyPromoClickIntents {

    fun onBackClick()

    fun onApyInfoClick()

    fun onHowItWorksClick()

    fun onStartEarningClick()

    fun onUrlClick(url: String)
}