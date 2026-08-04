package com.tangem.features.foryou.impl.components

/** 7% of the full circle — the minimum visual share any non-zero segment (and the grey gap) is drawn at. */
internal const val MIN_VISUAL_SWEEP_FRACTION = 0.07f

private const val FULL_CIRCLE_DEG = 360f

/** Share of the round cap width the last segment is compensated for at its lapped-over seams. */
private const val LAST_SEGMENT_CAP_COMP_FACTOR = 0.75f

/** Float-noise tolerance (deg): a remainder this small counts as "no gap" so an all-in ring stays full. */
private const val GREY_GAP_EPSILON_DEG = 0.01f

/**
 * Maps normalized segment [weights] (each expected in `0f..1f`) to sweep angles in degrees, guaranteeing
 * that every non-zero segment is drawn at least [MIN_VISUAL_SWEEP_FRACTION] of the full circle (7% → 25.2°),
 * so a tiny holding never collapses into an invisible sliver.
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
 * - **The grey gap (unfilled track remainder) obeys the same "floor or nothing" rule as a segment:** it is
 *   either absent (segments fill the whole ring, so `naturalGap ≈ 0`) or wide enough to *read* as at least
 *   [MIN_VISUAL_SWEEP_FRACTION] of the circle. A remainder thinner than that floor is grown to it by
 *   shrinking the segments (capping the segment budget at `360° − floor`), so the grey never renders as a
 *   hairline sliver. Because both neighbouring slices' round caps bulge into the grey, the reserved floor
 *   is padded by [capDeg] so the *visible* grey lands at [MIN_VISUAL_SWEEP_FRACTION]. Segments are never
 *   squeezed below their own floors to make room for it. [GREY_GAP_EPSILON_DEG] absorbs float noise so an
 *   ≈100% portfolio still reads as a full ring instead of snapping to a 7% grey gap.
 * - If there are so many segments that even the floor can't fit (`n * floor > 360°`), it falls back to an
 *   equal `360°/n` split.
 * - [capDeg] compensates the round-cap squeeze on the **last segment only** (see [lastSegmentOverlapDeg] for
 *   the angle), and **only on a full ring** (no grey gap): there slice 0's start cap laps over the last
 *   slice's end (a second seam beyond the one every slice has), so it loses ~[capDeg] more visible width.
 *   Once a grey gap exists the last slice has a free end and is no worse off than a middle slice, so no bump.
 *
 * @param capDeg round-cap overlap width in degrees. It feeds both the last-segment compensation and the grey
 *   gap padding, so it must match the geometry actually drawn: every caller involved in drawing, hit-testing
 *   or tooltip anchoring computes it with [lastSegmentOverlapDeg] from the same stroke and arc diameter.
 *   Pass `0f` only when there is no round cap (e.g. pure-geometry tests).
 *
 * The returned list has the same size and order as [weights].
 */
internal fun visualSweepAngles(weights: List<Float>, capDeg: Float): List<Float> {
    val base = weights.map { it.coerceIn(0f, 1f) * FULL_CIRCLE_DEG }
    val activeIndices = base.indices.filter { base[it] > 0f }
    val n = activeIndices.size
    if (n == 0) return List(weights.size) { 0f }

    val filledSum = activeIndices.sumOf { base[it].toDouble() }.toFloat()
    // Never demand more than an equal share when the ring can't fit every floor.
    val baseFloor = (MIN_VISUAL_SWEEP_FRACTION * FULL_CIRCLE_DEG).coerceAtMost(FULL_CIRCLE_DEG / n)

    // The grey gap follows the same floor-or-nothing rule as a segment: present ⇒ wide enough to *read* as
    // at least [baseFloor]. Unlike a segment, the grey has the round cap of BOTH neighbouring slices bulging
    // into it (and no grey cap of its own to overlap back), which eats ~[capDeg] of its span — so its floor
    // is the base floor plus that cap overlap, leaving [baseFloor] actually visible.
    val naturalGap = FULL_CIRCLE_DEG - filledSum
    val hasGrey = naturalGap > GREY_GAP_EPSILON_DEG
    val greyFloor = if (hasGrey) baseFloor + capDeg else 0f

    // Compensation for the LAST segment only, and only on a full ring (no grey gap). There the last slice is
    // lapped-over by a round cap at both seams (its start by the previous slice's end cap, its end by
    // slice 0's start cap), so it loses ~[capDeg] more visible width than the others. Once a grey gap exists
    // the last slice has a free end, so it's no worse off than a middle slice and gets no bump.
    val comp = if (hasGrey) 0f else capDeg * LAST_SEGMENT_CAP_COMP_FACTOR
    val lastActive = activeIndices.last()
    val floorOf = { index: Int ->
        if (index == lastActive) (baseFloor + comp).coerceAtMost(FULL_CIRCLE_DEG / n) else baseFloor
    }
    val floorsSum = activeIndices.sumOf { floorOf(it).toDouble() }.toFloat()
    // Segments occupy [budget]; the grey gap is the rest. Preserve the filled sweep when the floors fit,
    // grow into the track when they don't, then reserve [greyFloor] for the grey by capping at 360° − floor
    // — without ever pushing the segments below their own floors.
    val budget = maxOf(filledSum, floorsSum)
        .coerceAtMost(FULL_CIRCLE_DEG - greyFloor)
        .coerceAtLeast(floorsSum)
        .coerceAtMost(FULL_CIRCLE_DEG)

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

/**
 * Exact extra sweep (degrees) a round cap laps over one arc seam — the `capDeg` [visualSweepAngles] expects.
 *
 * A round cap bulges past its arc's angular end by one cap radius (`strokePx / 2`), i.e.
 * `capAngle = toDegrees((strokePx / 2) / R)` with `R = arcDiameter / 2` → `toDegrees(strokePx / arcDiameter)`.
 * A middle slice loses one such bulge at its start (covered by the previous slice's end cap) but keeps its
 * own end cap, so its visible width equals its sweep. The last slice on a full ring additionally has its end
 * covered by slice 0's start cap at the wrap — a second cap's worth — so it needs `2 × capAngle` back. The
 * grey gap likewise has both neighbouring caps bulging into it. Both compensations are sized off this value.
 *
 * [arcDiameter] is the ring centerline diameter — `min(width, height) − strokePx` in the draw/hit-test/tooltip
 * passes — so all three produce the same angles from the same stroke.
 */
internal fun lastSegmentOverlapDeg(strokePx: Float, arcDiameter: Float): Float =
    2f * Math.toDegrees((strokePx / arcDiameter).toDouble()).toFloat()