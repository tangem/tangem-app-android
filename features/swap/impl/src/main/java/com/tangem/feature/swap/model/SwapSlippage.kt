package com.tangem.feature.swap.model

import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.domain.acceptedSlippageOrNull
import java.math.BigDecimal
import java.math.RoundingMode

internal object SwapSlippage {

    const val DECIMALS = 2
    private val PRESET_FACTOR = BigDecimal(2)

    /** Presets around the default: half, the default itself and double. */
    fun presets(defaultPercent: BigDecimal): List<BigDecimal> {
        return listOf(
            defaultPercent.divide(PRESET_FACTOR, DECIMALS, RoundingMode.HALF_UP),
            defaultPercent,
            defaultPercent.multiply(PRESET_FACTOR),
        )
            .map(BigDecimal::stripTrailingZeros)
            .filter { it.signum() > 0 }
            .distinctBy(BigDecimal::toPlainString)
    }

    fun parseValid(value: String, provider: SwapProvider): BigDecimal? {
        return provider.acceptedSlippageOrNull(value.parseBigDecimalOrNull())
    }

    /** Below half the default the swap is likely to fail on price movement; the half preset itself is not low. */
    fun isLow(percent: BigDecimal, defaultPercent: BigDecimal): Boolean {
        return percent.multiply(PRESET_FACTOR) < defaultPercent
    }
}

sealed interface SwapSlippageState {

    val customOrNull: BigDecimal?
    val isResetHighlighted: Boolean

    /** [isResetHighlighted] marks a custom slippage reset to the default that the user hasn't seen yet. */
    data class Default(override val isResetHighlighted: Boolean = false) : SwapSlippageState {
        override val customOrNull: BigDecimal? get() = null
    }

    data class Custom(val value: BigDecimal, val quoteKey: SlippageQuoteKey?) : SwapSlippageState {
        override val customOrNull: BigDecimal get() = value
        override val isResetHighlighted: Boolean get() = false
    }
}

/** The pair and amount a custom slippage was picked for; a quote for anything else resets it. */
data class SlippageQuoteKey(
    val fromAccountId: AccountId,
    val fromCurrencyId: CryptoCurrency.ID,
    val toAccountId: AccountId,
    val toCurrencyId: CryptoCurrency.ID,
    val amount: String,
) {
    constructor(from: SwapCurrencyStatus, to: SwapCurrencyStatus, amount: String) : this(
        fromAccountId = from.account.accountId,
        fromCurrencyId = from.currency.id,
        toAccountId = to.account.accountId,
        toCurrencyId = to.currency.id,
        amount = amount,
    )
}

internal fun BigDecimal.formatSlippagePercent(): String = movePointLeft(2).format {
    percent(minFractionDigits = 0)
}