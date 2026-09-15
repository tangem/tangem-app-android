package com.tangem.features.introduction.impl.ui

/** The ProtoPie handoff, measured in milliseconds from the start of the video loop. */
internal object WelcomeTextTimeline {

    const val LOOP_LENGTH_MS = 12_917L

    private const val LINE_STAGGER_MS = 50
    private const val MOVE_MS = 500
    private const val FADE_MS = 300
    private const val FADE_IN_DELAY_MS = 150
    private const val FADE_OUT_DELAY_MS = 50
    private const val ENTER_OFFSET_DP = -10f
    private const val EXIT_OFFSET_DP = 8f

    private val scenes = listOf(
        Scene(enterAtMs = 0, exitAtMs = 3_100),
        Scene(enterAtMs = 3_400, exitAtMs = 6_800),
        Scene(enterAtMs = 7_100, exitAtMs = 12_400),
    )

    val sceneCount: Int = scenes.size

    val steady = Frame(alpha = 1f, offsetDp = 0f)

    val hidden = Frame(alpha = 0f, offsetDp = ENTER_OFFSET_DP)

    fun activeScene(positionMs: Long): Int = scenes.indexOfLast { positionMs >= it.enterAtMs }.coerceAtLeast(0)

    /**
     * The position to freeze the scenes at when playback stops. Between two scenes every line is transparent, so a
     * stop landing in one of those gaps would leave the screen with no text at all for the whole transition that
     * follows; such a position is rewound to the scene that faded out last, taken just before its exit. Any other
     * position is held exactly as it looked.
     */
    fun holdPosition(positionMs: Long): Long {
        if (scenes.indices.any { sceneAlpha(positionMs, scene = it) > 0f }) return positionMs
        val lastSettled = scenes.indexOfLast { positionMs >= it.settledAtMs }
        return scenes.getOrNull(lastSettled)?.exitAtMs?.toLong() ?: scenes.first().settledAtMs.toLong()
    }

    private fun sceneAlpha(positionMs: Long, scene: Int): Float =
        maxOf(frame(positionMs, scene, line = 0).alpha, frame(positionMs, scene, line = 1).alpha)

    fun frame(positionMs: Long, scene: Int, line: Int): Frame {
        val timing = scenes.getOrNull(scene) ?: return hidden
        val stagger = line * LINE_STAGGER_MS
        val enterAt = timing.enterAtMs + stagger
        val exitAt = timing.exitAtMs + stagger
        return when {
            positionMs < enterAt -> hidden
            positionMs < exitAt -> Frame(
                alpha = progress(positionMs, startMs = enterAt + FADE_IN_DELAY_MS, durationMs = FADE_MS),
                offsetDp = ENTER_OFFSET_DP * (1f - progress(positionMs, startMs = enterAt, durationMs = MOVE_MS)),
            )
            else -> Frame(
                alpha = 1f - progress(positionMs, startMs = exitAt + FADE_OUT_DELAY_MS, durationMs = FADE_MS),
                offsetDp = EXIT_OFFSET_DP * progress(positionMs, startMs = exitAt, durationMs = MOVE_MS),
            )
        }
    }

    /** Smoothstep share of an animation that started at [startMs]: the closest simple curve to ProtoPie's default. */
    @Suppress("MagicNumber")
    private fun progress(positionMs: Long, startMs: Int, durationMs: Int): Float {
        val linear = ((positionMs - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
        return linear * linear * (3f - 2f * linear)
    }

    data class Frame(val alpha: Float, val offsetDp: Float)

    private data class Scene(val enterAtMs: Int, val exitAtMs: Int) {

        /** The first moment the scene stands still, both of its lines fully in. */
        val settledAtMs: Int = enterAtMs + LINE_STAGGER_MS + FADE_IN_DELAY_MS + FADE_MS
    }
}