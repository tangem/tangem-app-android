package com.tangem.features.storiesv2.impl.content

/**
 * Last line of defence between a composition and the player: drop what cannot be shown, keep the rest, refuse to
 * open only when nothing showable is left. Bundled content goes through it too, which keeps the path exercised.
 */
internal object StoryV2CompositionValidator {

    private const val MAX_ACTIONS = 2

    /** @return the playable composition, or `null` when no slide survived. */
    fun validate(composition: StoryV2Composition): StoryV2Composition? {
        val slides = composition.slides
            .filter { it.asset.durationMs > 0 }
            .map { it.copy(actions = it.actions?.let(::sanitizeActions), hapticAtMs = sanitizeHaptics(it)) }

        if (slides.isEmpty()) return null

        return composition.copy(actions = sanitizeActions(composition.actions), slides = slides)
    }

    /** At most one action per style, at most two in total. Placement comes from the style, never from the order. */
    private fun sanitizeActions(actions: List<StoryV2Action>): List<StoryV2Action> {
        return actions.distinctBy(StoryV2Action::style).take(MAX_ACTIONS)
    }

    /**
     * Markers are offsets inside the slide, so anything at or past the duration is never reached and a duplicate
     * fires twice on one frame. Images have no timeline to hang a marker on.
     */
    private fun sanitizeHaptics(slide: StoryV2Slide): List<Long> {
        if (slide.asset is StoryV2Asset.Image) return emptyList()

        return slide.hapticAtMs
            .filter { it >= 0 && it < slide.asset.durationMs }
            .distinct()
            .sorted()
    }
}