package com.tangem.features.yield.supply.impl.promo.model

internal interface YieldSupplyPromoClickIntents {

    fun onBackClick()

    /** The plain variable-rate pill: the Aave supply history, with no campaign in it. */
    fun onVariableRateInfoClick()

    /** The campaign pill: the incentivised history, which is a different sheet, not a variant of the plain one. */
    fun onIncentiveRateInfoClick()

    fun onHowItWorksClick()

    fun onStartEarningClick()

    fun onUrlClick(url: String)
}