@file:Suppress("MagicNumber")

package com.tangem.features.storiesv2.impl.storybook

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.storiesv2.impl.R
import com.tangem.features.storiesv2.impl.content.BundledStoryV2ContentSource
import com.tangem.features.storiesv2.impl.content.StoryV2Action
import com.tangem.features.storiesv2.impl.content.StoryV2Asset
import com.tangem.features.storiesv2.impl.content.StoryV2Composition
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2EndBehavior
import com.tangem.features.storiesv2.impl.content.StoryV2MediaRef
import com.tangem.features.storiesv2.impl.content.StoryV2Origin
import com.tangem.features.storiesv2.impl.content.StoryV2Slide

/**
 * Compositions the tester showcase can play, each making one behaviour of the engine visible. Compositions rather
 * than screenshots because a seam, a stalled bar or tap-back on the first slide can only be seen by playing them.
 */
internal enum class StoryV2Preset(val title: String, val description: String) {

    ONBOARDING(
        title = "Onboarding",
        description = "The shipped composition: three videos into an image, looping, two actions",
    ),
    FINISH(
        title = "Finish behaviour",
        description = "Three videos, one action, ends after the last slide instead of looping",
    ),
    BARE(
        title = "No texts, no actions",
        description = "Nothing but the media and the progress strip",
    ),
    MIXED_MEDIA(
        title = "Mixed media",
        description = "Video, then a contained image, then a Lottie composition timed by the slide",
    ),
    LONG(
        title = "Eight slides",
        description = "Segments share the width once the fixed size no longer fits",
    ),
    SHORT_SLIDES(
        title = "One second slides",
        description = "Back-to-back transitions, the hardest case for a seam",
    ),
    HAPTIC(
        title = "Haptic markers",
        description = "Three markers per slide, silent while held or in background",
    ),
    BROKEN_ASSET(
        title = "Broken asset",
        description = "The file does not exist: playback fails and the slide continues on its poster",
    ),
    ;

    companion object {

        private val primaryAction = StoryV2Action(
            id = "scan_wallet",
            label = resourceReference(R.string.stories_onboarding_action_scan),
            style = StoryV2Action.Style.PRIMARY,
            target = StoryV2Action.Target.Screen(key = "scan_wallet"),
            icon = StoryV2MediaRef.DrawableResource(R.drawable.ic_tangem_24),
        )

        private val videoSources = listOf(
            R.raw.stories_onboarding_slide_1,
            R.raw.stories_onboarding_slide_2,
            R.raw.stories_onboarding_slide_3,
        )

        private val videoPosters = listOf(
            R.drawable.img_stories_onboarding_poster_1,
            R.drawable.img_stories_onboarding_poster_2,
            R.drawable.img_stories_onboarding_poster_3,
        )

        private val videoDurations = listOf(7_000L, 6_000L, 5_000L)

        fun build(preset: StoryV2Preset, bundled: BundledStoryV2ContentSource): StoryV2Composition = when (preset) {
            ONBOARDING -> bundled.onboarding()
            FINISH -> preview(
                slides = videoSlides(),
                endBehavior = StoryV2EndBehavior.FINISH,
                actions = listOf(primaryAction),
            )
            BARE -> preview(slides = videoSlides(withTexts = false))
            MIXED_MEDIA -> preview(
                slides = listOf(
                    videoSlide(index = 0),
                    imageSlide(contentMode = StoryV2ContentMode.CONTAIN),
                    lottieSlide(),
                ),
                actions = listOf(primaryAction),
            )
            LONG -> preview(slides = List(size = 8) { index -> videoSlide(index = index % 3, id = "long_$index") })
            SHORT_SLIDES -> preview(
                slides = List(size = 4) { index ->
                    videoSlide(index = index % 3, id = "short_$index", durationMs = 1_200L)
                },
            )
            HAPTIC -> preview(
                slides = videoSlides().map { it.copy(hapticAtMs = listOf(1_000L, 2_000L, 3_000L)) },
            )
            BROKEN_ASSET -> preview(
                slides = listOf(
                    videoSlide(index = 0),
                    brokenSlide(),
                    videoSlide(index = 2, id = "after_broken"),
                ),
            )
        }

        private fun preview(
            slides: List<StoryV2Slide>,
            endBehavior: StoryV2EndBehavior = StoryV2EndBehavior.LOOP,
            actions: List<StoryV2Action> = emptyList(),
        ) = StoryV2Composition(
            id = "storybook",
            version = 1,
            origin = StoryV2Origin.BUNDLED,
            endBehavior = endBehavior,
            actions = actions,
            slides = slides,
        )

        private fun videoSlides(withTexts: Boolean = true) = List(size = 3) { index ->
            videoSlide(index = index, withTexts = withTexts)
        }

        private fun videoSlide(
            index: Int,
            id: String = "video_$index",
            durationMs: Long = videoDurations[index],
            withTexts: Boolean = true,
        ) = StoryV2Slide(
            id = id,
            title = if (withTexts) stringReference("Slide ${index + 1}") else null,
            subtitle = if (withTexts) stringReference("Bundled video, $durationMs ms") else null,
            asset = StoryV2Asset.Video(
                source = StoryV2MediaRef.RawResource(videoSources[index]),
                poster = StoryV2MediaRef.DrawableResource(videoPosters[index]),
                durationMs = durationMs,
            ),
        )

        private fun imageSlide(contentMode: StoryV2ContentMode) = StoryV2Slide(
            id = "image",
            title = stringReference("Image slide"),
            subtitle = stringReference("Timed by the composition, ${contentMode.name.lowercase()}"),
            asset = StoryV2Asset.Image(
                source = StoryV2MediaRef.DrawableResource(R.drawable.img_stories_onboarding_slide_4),
                durationMs = 4_000L,
                contentMode = contentMode,
            ),
        )

        private fun lottieSlide() = StoryV2Slide(
            id = "lottie",
            title = stringReference("Vector animation"),
            subtitle = stringReference("Shorter than its segment, so it holds the last frame"),
            asset = StoryV2Asset.VectorAnimation(
                source = StoryV2MediaRef.RawResource(R.raw.anim_confetti),
                poster = StoryV2MediaRef.DrawableResource(videoPosters[0]),
                durationMs = 6_000L,
            ),
        )

        private fun brokenSlide() = StoryV2Slide(
            id = "broken",
            title = stringReference("Broken asset"),
            subtitle = stringReference("Playback failed, the poster carries the slide"),
            asset = StoryV2Asset.Video(
                source = StoryV2MediaRef.LocalFile(path = "/does/not/exist.mp4"),
                poster = StoryV2MediaRef.DrawableResource(videoPosters[1]),
                durationMs = 4_000L,
            ),
        )
    }
}