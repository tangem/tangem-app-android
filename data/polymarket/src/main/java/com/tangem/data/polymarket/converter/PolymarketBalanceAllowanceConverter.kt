package com.tangem.data.polymarket.converter

import com.tangem.datasource.api.polymarket.clob.models.PolymarketBalanceAllowanceResponse
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Converts the CLOB's balance/allowance response, whose amounts are decimal strings in the collateral's
 * smallest unit, into collateral amounts. An unparsable amount throws — the caller turns that into a typed
 * error rather than reporting a wrong balance.
 *
 * An absent allowance stays absent: substituting zero would report the one value that means "not approved".
 */
internal class PolymarketBalanceAllowanceConverter @Inject constructor() :
    Converter<PolymarketBalanceAllowanceResponse, PolymarketBalanceAllowance> {

    override fun convert(value: PolymarketBalanceAllowanceResponse): PolymarketBalanceAllowance =
        PolymarketBalanceAllowance(
            balance = value.balance.toCollateralAmount(),
            allowance = value.allowance?.toCollateralAmount(),
        )

    private fun String.toCollateralAmount(): BigDecimal = BigDecimal(this)
        .movePointLeft(PolymarketContracts.COLLATERAL_DECIMALS)
}