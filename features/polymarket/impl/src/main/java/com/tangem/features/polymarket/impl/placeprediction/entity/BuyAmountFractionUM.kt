package com.tangem.features.polymarket.impl.placeprediction.entity

internal enum class BuyAmountFractionUM(val percent: Int) {
    Quarter(percent = 25),
    Half(percent = 50),
    ThreeQuarters(percent = 75),
    Max(percent = 90),
}