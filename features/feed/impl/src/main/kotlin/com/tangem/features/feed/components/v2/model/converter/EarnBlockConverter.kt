package com.tangem.features.feed.components.v2.model.converter

import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.earn.ResolveEarnRateUseCase
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.features.feed.ui.v2.state.EarnBlockUM
import com.tangem.utils.StringsSigns.PERCENT
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal

/**
 * Picks what the Earn card says and where it leads, from the user's holdings across every wallet.
 *
 * A user who already stakes or lends anything is "growing": the card takes them to For You and, when a
 * holding is worth naming, pitches that token's rate. Otherwise the card onboards them into the Earn
 * tab with the best rate Earn offers at all. A grower with no nameable holding keeps the growth target
 * but falls back to the onboarding wording.
 *
 * @property yieldSupplyApy APY in percent per yield-supply key
 * @property stakingAvailability staking availability per currency, merged across wallets
 * @property catalogueApy best rate in the Earn catalogue as a fraction (`0.05` is 5%), `null` until loaded
 */
internal class EarnBlockConverter(
    private val yieldSupplyApy: Map<String, BigDecimal>,
    private val stakingAvailability: Map<CryptoCurrency, StakingAvailability>,
    private val catalogueApy: BigDecimal?,
) : Converter<List<CryptoCurrencyStatus>, EarnBlockUM> {

    private val resolveEarnRateUseCase = ResolveEarnRateUseCase()

    override fun convert(value: List<CryptoCurrencyStatus>): EarnBlockUM {
        val earnable = value.mapNotNull { status ->
            val rate = resolveEarnRateUseCase(
                currencyStatus = status,
                yieldSupplyApy = yieldSupplyApy,
                stakingAvailability = stakingAvailability,
            )
            if (rate == null) null else status to rate
        }

        val isEarning = earnable.any { (_, rate) -> rate.isActive }
        // Best rate first, and among equal rates the holding with the most money in it
        val nameable = earnable
            .filterNot { (status, _) -> status.value.fiatAmount.isNullOrZero() }
            .maxWithOrNull(
                compareBy(
                    { (_, rate) -> rate.apy },
                    { (status, _) -> status.value.fiatAmount.orZero() },
                ),
            )

        return EarnBlockUM(
            pitch = when {
                isEarning && nameable != null -> EarnBlockUM.Pitch.Growth(
                    apy = nameable.second.apy.formatPercent(),
                    tokenSymbol = nameable.first.currency.symbol,
                )
                catalogueApy != null -> EarnBlockUM.Pitch.Onboarding(apy = catalogueApy.formatPercent())
                else -> EarnBlockUM.Pitch.Plain
            },
            target = if (isEarning) EarnBlockUM.Target.ForYou else EarnBlockUM.Target.EarnTab,
        )
    }

    /** Fraction to the app-wide percent rendering, e.g. `0.1264` → `12.64%`. */
    private fun BigDecimal.formatPercent(): String = format { percent(withPercentSign = false) } + PERCENT
}