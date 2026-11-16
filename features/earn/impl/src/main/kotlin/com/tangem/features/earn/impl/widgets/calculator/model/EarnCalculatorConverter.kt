package com.tangem.features.earn.impl.widgets.calculator.model

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorTexts
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnCalculatorUM
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnHorizonItemUM
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnHorizonSelectUM
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnProjectionBarUM
import com.tangem.features.earn.impl.widgets.calculator.ui.state.EarnStepperUM
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnProjectionResult
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.MathContext
import javax.inject.Inject

internal class EarnCalculatorConverter @Inject constructor() {

    @Suppress("LongParameterList")
    fun convert(
        result: EarnProjectionResult,
        input: CalculatorInput,
        appCurrency: AppCurrency,
        incentiveRate: EarnProductRate?,
        onInitialStep: (Int) -> Unit,
        onTopUpStep: (Int) -> Unit,
        onHorizonSelect: (EarnHorizon) -> Unit,
    ): EarnCalculatorUM = EarnCalculatorUM(
        incentive = incentiveBadge(incentiveRate),
        total = stringReference(result.total.format { fiat(appCurrency.code, appCurrency.symbol) }),
        earned = stringReference(
            StringsSigns.PLUS + result.earned.format { fiat(appCurrency.code, appCurrency.symbol) },
        ),
        earnedLabel = EarnCalculatorTexts.earnedIn,
        horizon = horizonSelect(selected = input.horizon, onSelect = onHorizonSelect),
        bars = bars(result = result, input = input).toImmutableList(),
        axisLabels = axisLabels(input.horizon).toImmutableList(),
        initialBalance = stepper(
            label = EarnCalculatorTexts.initialBalance,
            value = input.initialFiat.wholeAmount(appCurrency),
            canDecrease = input.canDecreaseInitial,
            canIncrease = input.canIncreaseInitial,
            onStep = onInitialStep,
        ),
        monthlyTopUp = stepper(
            label = EarnCalculatorTexts.monthlyTopUp,
            value = input.monthlyTopUpFiat.wholeAmount(appCurrency),
            canDecrease = input.canDecreaseTopUp,
            canIncrease = input.canIncreaseTopUp,
            onStep = onTopUpStep,
        ),
    )

    /** A campaign without a reward component has nothing to announce, so a zero rate shows no badge. */
    private fun incentiveBadge(rate: EarnProductRate?): TangemBadgeUM? {
        val fraction = rate?.fraction?.takeIf { it.signum() > 0 } ?: return null

        return TangemBadgeUM(
            text = resourceReference(
                id = R.string.yield_module_promo_screen_incentivised_chip,
                formatArgs = wrappedList(fraction.format { percent(minFractionDigits = 0, canBeLower = true) }),
            ),
            status = TangemBadge.Status.Info,
            size = TangemBadge.Size.X6,
        )
    }

    private fun horizonSelect(selected: EarnHorizon, onSelect: (EarnHorizon) -> Unit) = EarnHorizonSelectUM(
        selected = EarnCalculatorTexts.horizon(selected),
        items = EarnHorizon.entries.map { horizon ->
            EarnHorizonItemUM(
                label = EarnCalculatorTexts.horizon(horizon),
                isSelected = horizon == selected,
                onClick = { onSelect(horizon) },
            )
        }.toImmutableList(),
    )

    private fun bars(result: EarnProjectionResult, input: CalculatorInput): List<EarnProjectionBarUM> {
        val points = result.points.sampled()
        val tallest = points.maxOf { point -> input.initialFiat + point.capital + input.monthlyTopUpFiat }

        val bars = points.map { point ->
            EarnProjectionBarUM(
                initial = input.initialFiat.visibleShareOf(tallest),
                total = point.capital.visibleShareOf(tallest),
                topUp = input.monthlyTopUpFiat.visibleShareOf(tallest),
            )
        }
        val tallestStack = bars.maxOf { it.initial + it.total + it.topUp }

        return if (tallestStack > 1f) {
            bars.map { bar ->
                EarnProjectionBarUM(
                    initial = bar.initial / tallestStack,
                    total = bar.total / tallestStack,
                    topUp = bar.topUp / tallestStack,
                )
            }
        } else {
            bars
        }
    }

    /** A non-empty layer never gets thinner than [MIN_LAYER_SHARE], otherwise a small amount is not drawn at all. */
    private fun BigDecimal.visibleShareOf(max: BigDecimal): Float {
        val share = shareOf(max)

        return if (share > 0f) maxOf(share, MIN_LAYER_SHARE) else 0f
    }

    private fun axisLabels(horizon: EarnHorizon): List<TextReference> = (1..AXIS_LABELS_COUNT).map { step ->
        val months = horizon.months * step / AXIS_LABELS_COUNT

        if (months % MONTHS_IN_YEAR == 0) {
            EarnCalculatorTexts.axisYears(months / MONTHS_IN_YEAR)
        } else {
            EarnCalculatorTexts.axisMonths(months)
        }
    }

    @Suppress("LongParameterList")
    private fun stepper(
        label: TextReference,
        value: String,
        canDecrease: Boolean,
        canIncrease: Boolean,
        onStep: (Int) -> Unit,
    ) = EarnStepperUM(
        label = label,
        value = stringReference(value),
        isMinusEnabled = canDecrease,
        isPlusEnabled = canIncrease,
        minusDescription = EarnCalculatorTexts.decrease(label),
        plusDescription = EarnCalculatorTexts.increase(label),
        onMinus = { onStep(-1) },
        onPlus = { onStep(1) },
    )

    private fun BigDecimal.wholeAmount(appCurrency: AppCurrency): String =
        format { fiat(appCurrency.code, appCurrency.symbol).optionalDecimals() }

    private fun BigDecimal.shareOf(max: BigDecimal): Float =
        if (max.signum() == 0) 0f else divide(max, MathContext.DECIMAL64).toFloat()

    private fun <T> List<T>.sampled(): List<T> {
        if (size <= MAX_BARS) return this

        return List(MAX_BARS) { bar -> this[((bar + 1) * size + MAX_BARS / 2) / MAX_BARS - 1] }
    }

    private companion object {
        const val MAX_BARS = 24
        const val MIN_LAYER_SHARE = 0.05f
        const val AXIS_LABELS_COUNT = 3
        const val MONTHS_IN_YEAR = 12
    }
}