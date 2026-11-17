package com.tangem.feature.tokendetails.presentation.tokendetails.model

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.AccruingBalanceContribution
import java.math.BigDecimal

/**
 * What the accruing ticker starts from. A restart rewinds the extrapolated growth to the last read, so the ticker
 * restarts only when these change; the freshness of a read is left out, a refresh with the same numbers keeps it going.
 */
internal data class AccruingTickInputs(
    val amount: BigDecimal?,
    val contributions: List<Pair<BigDecimal, BigDecimal?>>,
    val fiatRate: BigDecimal?,
    val appCurrency: AppCurrency,
) {

    companion object {

        fun of(status: CryptoCurrencyStatus, appCurrency: AppCurrency) = AccruingTickInputs(
            amount = status.value.amount,
            contributions = status.value.contributions.map { contribution ->
                contribution.totalDeltaCryptoAmount() to (contribution as? AccruingBalanceContribution)?.accrualRate
            },
            fiatRate = status.value.fiatRate,
            appCurrency = appCurrency,
        )
    }
}