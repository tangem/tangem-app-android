package com.tangem.core.ui.components.marketprice

data class PriceChangeConfig(val valueInPercent: String, val type: Type) {

    /** Price changing type */
    enum class Type {
        UP, DOWN
    }
}