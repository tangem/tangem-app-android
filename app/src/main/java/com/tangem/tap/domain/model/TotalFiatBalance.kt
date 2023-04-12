package com.tangem.tap.domain.model

import java.math.BigDecimal

/**
 * Represents fiat balance of [WalletStoreModel] list
 * @property amount Amount of the total balance
 * */
sealed interface TotalFiatBalance {
    val amount: BigDecimal?

    object Loading : TotalFiatBalance {
        override val amount: BigDecimal? = null
    }

    object Failed : TotalFiatBalance {
        override val amount: BigDecimal? = null
    }

    data class Loaded(
        override val amount: BigDecimal,
        val isWarning: Boolean,
    ) : TotalFiatBalance
}
