@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.polymarket.crypto

import com.tangem.feature.tester.presentation.storybook.entity.PolymarketCryptoChartStory as Story
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart as Chart
import kotlinx.collections.immutable.toImmutableList
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.sin

internal fun cryptoFixture(sample: Story.Sample, interval: Story.Interval, target: Story.Target): Chart.Snapshot {
    val base = when (sample) {
        Story.Sample.Ethereum -> 2_540.0
        Story.Sample.Narrowing -> 2_530.0
        Story.Sample.Tiny -> 0.000008
        else -> 75_640.0
    }
    val amplitude = base / 2_000
    val points = if (sample == Story.Sample.Narrowing) {
        listOf(
            Chart.Point(CRYPTO_FIXTURE_START, base - 30),
            Chart.Point(CRYPTO_FIXTURE_START + 1_000, base),
            Chart.Point(CRYPTO_FIXTURE_START + interval.millis, base),
        )
    } else {
        List(61) { index ->
            val change = if (sample == Story.Sample.Flat) {
                0.0
            } else {
                amplitude * (sin(index * 0.13) + sin(index * 0.7) * 0.12 + index / 40.0)
            }
            Chart.Point(
                timestampMillis = CRYPTO_FIXTURE_START + interval.millis * index / 60,
                price = base + change,
                isSegmentStart = sample == Story.Sample.Gap && index == 40,
            )
        }.filterIndexed { index, _ ->
            sample.includes(index)
        }
    }
    return Chart.Snapshot(
        points = points.toImmutableList(),
        range = Chart.TimeRange(CRYPTO_FIXTURE_START, CRYPTO_FIXTURE_START + interval.millis),
        targetPrice = when (target) {
            Story.Target.Inside -> base + amplitude
            Story.Target.Above -> base + amplitude * 10
            Story.Target.Below -> base - amplitude * 10
            Story.Target.Hidden -> null
        },
    )
}

internal class CryptoDemoPriceFormatter {
    private val formatters = mutableMapOf<Int, NumberFormat>()

    fun format(value: Double, digits: Int): String = formatters.getOrPut(digits) {
        NumberFormat.getCurrencyInstance(Locale.US).apply {
            currency = Currency.getInstance("USD")
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
    }.format(value)
}

internal const val CRYPTO_FIXTURE_START = 1_793_613_600_000L

private fun Story.Sample.includes(index: Int): Boolean = when (this) {
    Story.Sample.Empty -> false
    Story.Sample.Single -> index == 30
    Story.Sample.Gap -> index !in 20..39
    Story.Sample.Stale -> index <= 25
    else -> true
}

internal fun liveCryptoPoints(snapshot: Chart.Snapshot, revision: Int, mode: Story.UpdateMode) =
    (snapshot.points + List(revision) { index ->
        val last = snapshot.points.lastOrNull()?.price ?: 75_000.0
        val elapsed = index * mode.cadenceMillis
        val wave = sin(elapsed * 0.0003) * last / 2_000
        val jump = if (mode == Story.UpdateMode.Jump && elapsed >= 3_000) last * 0.01 else 0.0
        Chart.Point(snapshot.range.endMillis + elapsed + mode.cadenceMillis, last + wave + jump)
    }.filterIndexed { index, _ -> mode != Story.UpdateMode.Sparse || index % 6 == 0 }).toImmutableList()