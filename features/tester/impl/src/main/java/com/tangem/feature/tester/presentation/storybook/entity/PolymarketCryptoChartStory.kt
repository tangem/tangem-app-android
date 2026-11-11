@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.entity

internal data class PolymarketCryptoChartStory(
    val isCompact: Boolean,
    val isLive: Boolean,
    val updateMode: UpdateMode,
    val sample: Sample,
    val interval: Interval,
    val target: Target,
    val isDark: Boolean,
    val fontScale: Float,
    val onCompact: (Boolean) -> Unit,
    val onLive: (Boolean) -> Unit,
    val onUpdateMode: (UpdateMode) -> Unit,
    val onSample: (Sample) -> Unit,
    val onInterval: (Interval) -> Unit,
    val onTarget: (Target) -> Unit,
    val onDark: (Boolean) -> Unit,
    val onFontScale: (Float) -> Unit,
) : StoryBookPage {
    enum class UpdateMode(val cadenceMillis: Long) { Regular(1_000), Sparse(1_000), Jump(1_000), Fast(16) }
    enum class Sample { Bitcoin, Ethereum, Tiny, Empty, Single, Flat, Gap, Stale, Narrowing }
    enum class Interval(val label: String, val millis: Long) {
        Minute("1 min", 60_000),
        QuarterHour("15 min", 900_000),
        Hour("1 hour", 3_600_000),
        Day("1 day", 86_400_000),
    }
    enum class Target { Inside, Above, Below, Hidden }
}