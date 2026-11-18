package com.tangem.features.earn.impl.widgets.calculator.model

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.impl.ui.toIncentiveUM
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
        onIncentiveClick: () -> Unit,
        onInitialStep: (Int) -> Unit,
        onTopUpStep: (Int) -> Unit,
        onHorizonSelect: (EarnHorizon) -> Unit,
        onBarSelected: (Int?) -> Unit,
    ): EarnCalculatorUM = EarnCalculatorUM(
        incentive = incentiveRate.toIncentiveUM(onClick = onIncentiveClick),
        total = stringReference(result.total.format { fiat(appCurrency.code, appCurrency.symbol) }),
        earned = stringReference(
            StringsSigns.PLUS + result.earned.format { fiat(appCurrency.code, appCurrency.symbol) },
        ),
        earnedLabel = resourceReference(R.string.earn_calculator_earned_in_label),
        horizon = horizonSelect(selected = input.horizon, onSelect = onHorizonSelect),
        bars = bars(result = result, appCurrency = appCurrency).toImmutableList(),
        axisLabels = axisLabels(input.horizon).toImmutableList(),
        selectedBar = null,
        onBarSelected = onBarSelected,
        initialBalance = stepper(
            label = resourceReference(R.string.earn_calculator_initial_balance),
            value = input.initialFiat.wholeAmount(appCurrency),
            canDecrease = input.canDecreaseInitial,
            canIncrease = input.canIncreaseInitial,
            onStep = onInitialStep,
        ),
        monthlyTopUp = stepper(
            label = resourceReference(R.string.earn_calculator_monthly_top_up),
            value = input.monthlyTopUpFiat.wholeAmount(appCurrency),
            canDecrease = input.canDecreaseTopUp,
            canIncrease = input.canIncreaseTopUp,
            onStep = onTopUpStep,
        ),
    )

    private fun horizonSelect(selected: EarnHorizon, onSelect: (EarnHorizon) -> Unit) = EarnHorizonSelectUM(
        selected = horizonText(selected),
        items = EarnHorizon.entries.map { horizon ->
            EarnHorizonItemUM(
                label = horizonText(horizon),
                isSelected = horizon == selected,
                onClick = { onSelect(horizon) },
            )
        }.toImmutableList(),
    )

    private fun horizonText(horizon: EarnHorizon): TextReference = if (horizon.months % MONTHS_IN_YEAR == 0) {
        val years = horizon.months / MONTHS_IN_YEAR
        pluralReference(R.plurals.earn_calculator_years, count = years, formatArgs = wrappedList(years))
    } else {
        pluralReference(
            id = R.plurals.earn_calculator_months,
            count = horizon.months,
            formatArgs = wrappedList(horizon.months),
        )
    }

    private fun bars(result: EarnProjectionResult, appCurrency: AppCurrency): List<EarnProjectionBarUM> {
        val points = result.points.sampled()
        val tallest = points.maxOf { point -> point.income }

        return points.map { point ->
            EarnProjectionBarUM(
                height = point.income.visibleShareOf(tallest),
                balance = stringReference(point.capital.format { fiat(appCurrency.code, appCurrency.symbol) }),
                earned = stringReference(
                    StringsSigns.PLUS + point.income.format { fiat(appCurrency.code, appCurrency.symbol) },
                ),
            )
        }
    }

    /** A non-empty bar never gets shorter than [MIN_BAR_SHARE], otherwise a small amount is not drawn at all. */
    private fun BigDecimal.visibleShareOf(max: BigDecimal): Float {
        val share = shareOf(max)

        return if (share > 0f) maxOf(share, MIN_BAR_SHARE) else 0f
    }

    private fun axisLabels(horizon: EarnHorizon): List<TextReference> = (1..AXIS_LABELS_COUNT).map { step ->
        val months = horizon.months * step / AXIS_LABELS_COUNT

        if (months % MONTHS_IN_YEAR == 0) {
            resourceReference(R.string.earn_calculator_axis_years, wrappedList(months / MONTHS_IN_YEAR))
        } else {
            resourceReference(R.string.earn_calculator_axis_months, wrappedList(months))
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
        minusDescription = resourceReference(R.string.earn_calculator_decrease_accessibility, wrappedList(label)),
        plusDescription = resourceReference(R.string.earn_calculator_increase_accessibility, wrappedList(label)),
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
        const val MIN_BAR_SHARE = 0.05f
        const val AXIS_LABELS_COUNT = 3
        const val MONTHS_IN_YEAR = 12
    }
}