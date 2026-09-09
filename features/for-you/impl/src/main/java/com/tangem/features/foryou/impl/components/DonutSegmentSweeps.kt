package com.tangem.features.foryou.impl.components

/**
 * Minimum visual share, as a fraction of the full circle, that any non-zero segment (and the grey gap) is
 * drawn at. A tuning knob — nothing should restate its value, in docs or in tests.
 *
 */
internal const val MIN_VISUAL_SWEEP_FRACTION = 0.01f

private const val FULL_CIRCLE_DEG = 360f

/** Float-noise tolerance (deg): a remainder this small counts as "no gap" so an all-in ring stays full. */
private const val GREY_GAP_EPSILON_DEG = 0.01f

/** Tolerance (deg) for calling the ring closed, so an exact-complement slice leaves no hairline of track. */
private const val CLOSED_RING_EPSILON_DEG = 0.01f

/**
 * Maps normalized segment [weights] (each expected in `0f..1f`) to sweep angles in degrees, guaranteeing
 * that every non-zero segment is drawn at least [MIN_VISUAL_SWEEP_FRACTION] of the full circle, so a tiny
 * holding never collapses into an invisible sliver.
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
 *   ≈100% portfolio still reads as a full ring instead of snapping to a floored grey gap.
 * - If there are so many segments that even the floor can't fit (`n * floor > 360°`), it falls back to an
 *   equal `360°/n` split.
 * - No segment is widened to pay for a round cap. A cap extends only forward, over the slice's successor,
 *   so every slice's visible width is exactly its sweep, however narrow. [capDeg] affects only the grey
 *   gap, which has two caps reaching into it and none of its own — see [capPaddingDeg].
 *
 * @param capDeg round-cap overlap width in degrees, for the grey gap padding. It must match the geometry
 *   actually drawn: every caller involved in drawing, hit-testing or tooltip anchoring computes it with
 *   [capPaddingDeg] from the same stroke and arc diameter. Pass `0f` only when there is no round
 *   cap (e.g. pure-geometry tests).
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

    val floorsSum = baseFloor * n
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
            pinned.forEach { result[it] = baseFloor }
            break
        }
        val freeBudget = budget - baseFloor * pinned.size
        val freeBaseSum = freeIndices.sumOf { base[it].toDouble() }.toFloat()
        freeIndices.forEach { result[it] = freeBudget * base[it] / freeBaseSum }

        val newlyBelow = freeIndices.filter { result[it] < baseFloor }
        if (newlyBelow.isEmpty()) {
            pinned.forEach { result[it] = baseFloor }
            break
        }
        pinned.addAll(newlyBelow)
    }
    return result
}

/**
 * Whether the slices leave no room for the track — i.e. their sweeps span the whole circle. The track is
 * translucent, so a caller must skip it here rather than paint it under a covering slice and darken it
 * twice.
 */
internal fun isRingClosed(sweeps: List<Float>): Boolean = sweeps.sum() >= FULL_CIRCLE_DEG - CLOSED_RING_EPSILON_DEG

/**
 * Index of the slice whose **start** is a free end rather than a seam — the first drawn slice of a ring the
 * slices don't close, which keeps a backward cap so the ring's tail stays rounded against the track.
 *
 * `null` on a closed ring: there the last slice's forward cap covers that point, so nothing is left to
 * round. Every slice's *end* always carries a forward cap, so it needs no such question.
 */
internal fun freeStartSliceIndex(sweeps: List<Float>): Int? =
    sweeps.indices.firstOrNull { sweeps[it] > 0f }?.takeIf { !isRingClosed(sweeps) }

/**
 * The indices to paint, back to front: the first entry ends up at the bottom and the last on top — so
 * slice 0, the largest holding, sits above its neighbour and its round end cap tucks over it. Slices with
 * nothing to draw are left out, so the caller paints every index it is handed.
 *
 * The caps are painted in a second pass over this same order, after every body — which is what lets the
 * last slice's forward cap land on slice 0 without the wrap needing a special case.
 */
internal fun slicePaintOrder(sweeps: List<Float>): List<Int> = sweeps.indices.filter { sweeps[it] > 0f }.reversed()

/**
 * Width (degrees) of the two round caps that bulge into an unfilled remainder — the `capDeg`
 * [visualSweepAngles] expects, and its only use.
 *
 * A round cap bulges past its arc's angular end by one cap radius (`strokePx / 2`), i.e.
 * `capAngle = toDegrees((strokePx / 2) / R)` with `R = arcDiameter / 2` → `toDegrees(strokePx / arcDiameter)`.
 * A gap has two such bulges reaching into it and no cap of its own — the last slice's forward cap at one
 * end, the first slice's backward cap at the other ([freeStartSliceIndex]) — hence twice that angle.
 *
 * The slices themselves need no allowance: a cap extends only *forward*, over the slice's successor, so
 * every slice's visible width is exactly its sweep, however narrow.
 *
 * [arcDiameter] is the ring centerline diameter — `min(width, height) − strokePx` in the draw/hit-test/tooltip
 * passes — so all three produce the same angles from the same stroke.
 */
internal fun capPaddingDeg(strokePx: Float, arcDiameter: Float): Float =
    2f * Math.toDegrees((strokePx / arcDiameter).toDouble()).toFloat()