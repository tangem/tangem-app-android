package com.tangem.features.introduction.impl.ui

/**
 * The ProtoPie handoff in milliseconds from the start of the video loop: every scene slides its lines in from above
 * while fading them in, holds, then slides them further down while fading out, each line 50 ms behind the previous.
 */
internal object WelcomeTextTimeline {

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

    val sceneCount: Int get() = scenes.size

    val steady = Frame(alpha = 1f, offsetDp = 0f)

    fun frame(positionMs: Long, scene: Int, line: Int): Frame {
        val stagger = line * LINE_STAGGER_MS
        val enterAt = scenes[scene].enterAtMs + stagger
        val exitAt = scenes[scene].exitAtMs + stagger
        return when {
            positionMs < enterAt -> Frame(alpha = 0f, offsetDp = ENTER_OFFSET_DP)
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

    private data class Scene(val enterAtMs: Int, val exitAtMs: Int)
}