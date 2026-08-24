package com.tangem.features.storiesv2.impl.content

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.tangem.core.ui.extensions.TextReference

/**
 * A story as the player sees it: an ordered set of slides, already resolved to assets that exist on this device.
 *
 * Deliberately not the shape of the remote response — the data layer resolves every remote url to a downloaded file
 * before building this, so the player behaves identically whether the story shipped in the apk or arrived over the
 * network.
 *
 * @property id       stable slug, also the analytics identity
 * @property version  bumped by any change to the composition
 * @property origin   reported to analytics
 * @property actions  inherited by slides that do not override them
 * @property slides   at least one; slides that could not be resolved are already filtered out
 */
internal data class StoryV2Composition(
    val id: String,
    val version: Int,
    val origin: StoryV2Origin,
    val endBehavior: StoryV2EndBehavior,
    val actions: List<StoryV2Action>,
    val slides: List<StoryV2Slide>,
) {

    fun actionsOf(slide: StoryV2Slide): List<StoryV2Action> = slide.actions ?: actions
}

internal enum class StoryV2Origin(val analyticsValue: String) {
    BUNDLED(analyticsValue = "bundled"),
    REMOTE(analyticsValue = "remote"),
}

internal enum class StoryV2EndBehavior {

    /** Report completion after the last slide and let the host move on. */
    FINISH,

    /** Restart from the first slide. The story never ends on its own. */
    LOOP,
}

/**
 * @property actions     `null` inherits the story actions; an empty list hides them on this slide
 * @property hapticAtMs  offsets from the slide start, each strictly below the asset duration
 */
internal data class StoryV2Slide(
    val id: String,
    val title: TextReference?,
    val subtitle: TextReference?,
    val asset: StoryV2Asset,
    val actions: List<StoryV2Action>? = null,
    val hapticAtMs: List<Long> = emptyList(),
)

/**
 * @property durationMs  authoritative display duration; progress, analytics and haptics are all measured against it,
 *                       never against the length the media file happens to have
 * @property poster      always present, so no code path can end up with nothing to draw
 */
internal sealed interface StoryV2Asset {

    val durationMs: Long
    val contentMode: StoryV2ContentMode
    val poster: StoryV2MediaRef

    data class Video(
        val source: StoryV2MediaRef,
        override val poster: StoryV2MediaRef,
        override val durationMs: Long,
        override val contentMode: StoryV2ContentMode = StoryV2ContentMode.COVER,
    ) : StoryV2Asset

    data class Image(
        val source: StoryV2MediaRef,
        override val durationMs: Long,
        override val contentMode: StoryV2ContentMode = StoryV2ContentMode.COVER,
    ) : StoryV2Asset {

        override val poster: StoryV2MediaRef get() = source
    }

    /** [durationMs] governs the segment, not the length of the animation. */
    data class VectorAnimation(
        val source: StoryV2MediaRef,
        override val poster: StoryV2MediaRef,
        override val durationMs: Long,
        override val contentMode: StoryV2ContentMode = StoryV2ContentMode.CONTAIN,
    ) : StoryV2Asset
}

internal enum class StoryV2ContentMode { COVER, CONTAIN }

/**
 * Where a resolved asset physically lives. No remote-url case on purpose: a url never reaches the player, the data
 * layer downloads it first and hands over [LocalFile].
 */
internal sealed interface StoryV2MediaRef {

    data class RawResource(@RawRes val id: Int) : StoryV2MediaRef

    data class DrawableResource(@DrawableRes val id: Int) : StoryV2MediaRef

    data class LocalFile(val path: String) : StoryV2MediaRef
}

/** @property icon bundled content only — the remote contract has no icon field, so a remote action is text alone */
internal data class StoryV2Action(
    val id: String,
    val label: TextReference,
    val style: Style,
    val target: Target,
    val icon: StoryV2MediaRef.DrawableResource? = null,
) {

    /** Placement is decided by the design system, not by the order actions arrive in. */
    enum class Style { PRIMARY, SECONDARY }

    sealed interface Target {

        data class Screen(val key: String) : Target

        data class Web(val url: String) : Target

        data class Deeplink(val uri: String) : Target

        data object Close : Target
    }
}