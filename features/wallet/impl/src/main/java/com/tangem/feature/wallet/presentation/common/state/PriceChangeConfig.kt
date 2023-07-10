package com.tangem.feature.wallet.presentation.common.state

/**
 * Price changing config
 *
 * @property valueInPercent value in percent
 * @property type           type [Type]
 */
internal data class PriceChangeConfig(val valueInPercent: String, val type: Type) {

    /** Price changing type */
    enum class Type {
        UP, DOWN
    }
}