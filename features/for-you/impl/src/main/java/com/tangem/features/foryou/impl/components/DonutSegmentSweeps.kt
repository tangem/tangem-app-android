package com.tangem.features.foryou.impl.components

/** 7% of the full circle — the minimum visual share any non-zero segment is drawn at. */
internal const val MIN_VISUAL_SWEEP_FRACTION = 0.07f

private const val FULL_CIRCLE_DEG = 360f

/** Share of the round cap width the last segment is compensated for at its lapped-over seams. */
private const val LAST_SEGMENT_CAP_COMP_FACTOR = 0.75f

/**
 * Maps normalized segment [weights] (each expected in `0f..1f`) to sweep angles in degrees, guaranteeing
 * that every non-zero segment is drawn at least [minFraction] of the full circle (default 5% → 18°), so a
 * tiny holding never collapses into an invisible sliver.
 *
 * This is a purely **visual** transform: the returned angles drive the arc drawing, hit-testing, and the
 * tooltip anchor. The real share shown in the tooltip must still come from the original `weight`.
 *
 * Rules:
 * - Zero-weight segments always map to `0f` (the drawing / hit-test passes skip them).
 * - Space for the bumped-up small segments is taken **proportionally** from the segments that are above the
 *   floor, so their relative proportions are preserved.
 * - The total filled sweep (and therefore the unfilled track remainder) is kept unchanged whenever the
 *   floors fit inside it; it only grows into the track if the floors genuinely demand more room.
 * - If there are so many segments that even the floor can't fit (`n * floor > 360°`), it falls back to an
 *   equal `360°/n` split.
 * - [capDeg] compensates the round-cap squeeze on the **last segment only** (see [DonutChart] for the
 *   angle). The bump is `max(0, capDeg − gap)`: full on a complete ring (where the last slice is lapped
 *   over at both seams), tapering as the unfilled track gap grows and reaching zero once the gap ≥ capDeg —
 *   past that the last slice has a free end and is no worse off than a middle slice. Other slices lap over
 *   on one side and lose nothing net, so they're never bumped.
 *
 * The returned list has the same size and order as [weights].
 */
internal fun visualSweepAngles(
    weights: List<Float>,
    minFraction: Float = MIN_VISUAL_SWEEP_FRACTION,
    capDeg: Float = 0f,
): List<Float> {
    val base = weights.map { it.coerceIn(0f, 1f) * FULL_CIRCLE_DEG }
    val activeIndices = base.indices.filter { base[it] > 0f }
    val n = activeIndices.size
    if (n == 0) return List(weights.size) { 0f }

    val filledSum = activeIndices.sumOf { base[it].toDouble() }.toFloat()
    // Never demand more than an equal share when the ring can't fit every floor.
    val baseFloor = (minFraction * FULL_CIRCLE_DEG).coerceAtMost(FULL_CIRCLE_DEG / n)
    // Compensation for the LAST segment only. On a full ring it's the one slice lapped-over by a round cap
    // at both seams (its start by the previous slice's end cap, its end by slice 0's start cap), so it
    // loses ~[capDeg] more visible width than the others. As a track gap opens, slice 0's start cap reaches
    // its end less, so that extra loss shrinks linearly with the gap and hits zero once the gap ≥ capDeg —
    // then the last slice is no worse off than a middle one, so no bump.
    val gap = FULL_CIRCLE_DEG - filledSum
    val comp = (capDeg * LAST_SEGMENT_CAP_COMP_FACTOR - gap).coerceAtLeast(0f)
    val lastActive = activeIndices.last()
    val floorOf = { index: Int ->
        if (index == lastActive) (baseFloor + comp).coerceAtMost(FULL_CIRCLE_DEG / n) else baseFloor
    }
    // Preserve the filled sweep when the floors fit; otherwise grow just enough to satisfy them.
    val floorsSum = activeIndices.sumOf { floorOf(it).toDouble() }.toFloat()
    val budget = maxOf(filledSum, floorsSum).coerceAtMost(FULL_CIRCLE_DEG)

    val result = MutableList(weights.size) { 0f }
    val pinned = HashSet<Int>()

    // Water-filling: repeatedly pin below-floor segments to their floor and re-split the rest
    // proportionally, until no free segment falls below its floor. Converges in ≤ n iterations.
    while (true) {
        val freeIndices = activeIndices.filter { it !in pinned }
        if (freeIndices.isEmpty()) {
            pinned.forEach { result[it] = floorOf(it) }
            break
        }
        val freeBudget = budget - pinned.sumOf { floorOf(it).toDouble() }.toFloat()
        val freeBaseSum = freeIndices.sumOf { base[it].toDouble() }.toFloat()
        freeIndices.forEach { result[it] = freeBudget * base[it] / freeBaseSum }

        val newlyBelow = freeIndices.filter { result[it] < floorOf(it) }
        if (newlyBelow.isEmpty()) {
            pinned.forEach { result[it] = floorOf(it) }
            break
        }
        pinned.addAll(newlyBelow)
    }
    return result
}