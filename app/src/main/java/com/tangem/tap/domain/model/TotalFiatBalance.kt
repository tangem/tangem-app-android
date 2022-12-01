package com.tangem.tap.domain.model

import java.math.BigDecimal

/**
 * Represents fiat balance of [WalletStoreModel] list
 * @property amount Amount of the total balance
 * */
sealed class TotalFiatBalance {
    open val amount: BigDecimal = BigDecimal.ZERO

    object Loading : TotalFiatBalance()

    class Refreshing(
        override val amount: BigDecimal,
    ) : TotalFiatBalance()

    class Error(
        override val amount: BigDecimal,
    ) : TotalFiatBalance()

    class Loaded(
        override val amount: BigDecimal,
    ) : TotalFiatBalance()
}