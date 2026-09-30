package com.tangem.features.storiesv2.impl.content.remote

import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.features.storiesv2.impl.content.StoryV2Haptic
import kotlin.math.roundToLong

/**
 * Turns the contract's flat list of haptic marks into bursts, the way iOS plays them.
 *
 * Marks closer than [CLUSTER_GAP_MS] read as one texture, and playing them one by one would not survive anyway: the
 * player fires markers once per frame and a new one cancels the burst still in flight, so a 30 ms series would lose
 * most of its pulses.
 */
internal object StoryV2HapticClusterizer {

    const val CLUSTER_GAP_MS = 150L

    fun cluster(marksMs: List<Long>): List<StoryV2Haptic> {
        val clusters = mutableListOf<MutableList<Long>>()
        marksMs.filter { it >= 0 }.distinct().sorted().forEach { mark ->
            val current = clusters.lastOrNull()
            if (current != null && mark - current.last() < CLUSTER_GAP_MS) {
                current.add(mark)
            } else {
                clusters.add(mutableListOf(mark))
            }
        }

        return clusters.map(::toHaptic)
    }

    /** Exact for pairs and evenly spaced series; an uneven series keeps its first and last pulse in place. */
    private fun toHaptic(marks: List<Long>): StoryV2Haptic {
        val pulses = marks.size
        val intervalMs = if (pulses > 1) {
            ((marks.last() - marks.first()).toDouble() / (pulses - 1)).roundToLong()
        } else {
            0L
        }
        return StoryV2Haptic(
            atMs = marks.first(),
            effect = TangemHapticEffect.OneTime.Tick,
            pulses = pulses,
            pulseIntervalMs = intervalMs,
        )
    }
}