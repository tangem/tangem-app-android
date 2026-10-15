package com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard

internal data class RewardCardTiming(
    val growMs: Int,
    val settleMs: Int,
    val spinMs: Int,
    val appearMs: Int,
    val chromeMs: Int,
    val startScale: Float,
    val peakScale: Float,
    val startShift: Float,
    val peakShift: Float,
) {

    companion object {

        val Default = RewardCardTiming(
            growMs = 2_000,
            settleMs = 1_500,
            spinMs = 2_750,
            appearMs = 2_000,
            chromeMs = 450,
            startScale = 0.5f,
            peakScale = 1.2f,
            startShift = 0.221f,
            peakShift = -0.053f,
        )

        /** After a skip the card lands on CARD_SHORT_VER, 2.0s after the reveal, which the sound is cut to. */
        val Skipped = Default.copy(
            growMs = 1_140,
            settleMs = 860,
            spinMs = 1_570,
            appearMs = 1_140,
        )
    }
}