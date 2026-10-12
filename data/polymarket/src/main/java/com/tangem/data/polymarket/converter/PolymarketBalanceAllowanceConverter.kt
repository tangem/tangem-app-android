package com.tangem.data.polymarket.converter

import com.tangem.grow.datasource.polymarket.clob.models.PolymarketBalanceAllowanceResponse
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal object PolymarketBalanceAllowanceConverter :
    Converter<PolymarketBalanceAllowanceResponse, PolymarketBalanceAllowance> {

    override fun convert(value: PolymarketBalanceAllowanceResponse): PolymarketBalanceAllowance =
        PolymarketBalanceAllowance(
            balance = value.balance.toCollateralAmount(),
            allowances = value.allowances?.mapKeys { it.key.lowercase() }
                ?.mapValues { it.value.toCollateralAmount() },
        )

    /**
     * The scale is normalised because `BigDecimal` carries it into `equals`: left as it comes out of the
     * shift, a zero balance is `0.000000`, which is neither equal to `BigDecimal.ZERO` nor to the `0` a
     * caller would write in a fixture. Stripping alone would overshoot into a negative scale for round
     * amounts (`1000.000000` becomes `1E+3`), so the scale is clamped back to zero.
     */
    private fun String.toCollateralAmount(): BigDecimal {
        val amount = BigDecimal(this)
            .movePointLeft(PolymarketContracts.COLLATERAL_DECIMALS)
            .stripTrailingZeros()

        return amount.setScale(maxOf(amount.scale(), 0))
    }
}