package com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard

internal object CardChoreography {

    const val SETTLED_SCALE = 1f

    const val SPIN_DEGREES = 360f

    /** Scale the case starts at as it grows in behind the card. */
    const val FRAME_START_SCALE = 0.8f

    fun growScale(grow: Float, timing: RewardCardTiming): Float = lerp(timing.startScale, timing.peakScale, grow)

    fun settleScale(settle: Float, timing: RewardCardTiming): Float = lerp(timing.peakScale, SETTLED_SCALE, settle)

    fun growShift(grow: Float, timing: RewardCardTiming): Float = lerp(timing.startShift, timing.peakShift, grow)

    fun settleShift(settle: Float, timing: RewardCardTiming): Float = lerp(timing.peakShift, 0f, settle)

    fun frameScale(settle: Float): Float = lerp(FRAME_START_SCALE, SETTLED_SCALE, settle)

    fun spinDegrees(spin: Float): Float = SPIN_DEGREES * spin.coerceIn(0f, 1f)

    private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
}