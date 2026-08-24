package com.tangem.features.storiesv2.impl.content

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import org.junit.jupiter.api.Test

internal class StoryV2CompositionValidatorTest {

    @Test
    fun `GIVEN a slide with a non-positive duration WHEN validated THEN the slide is dropped`() {
        // Arrange
        val composition = composition(
            slides = listOf(videoSlide(id = "good"), videoSlide(id = "bad", durationMs = 0L)),
        )

        // Act
        val actual = StoryV2CompositionValidator.validate(composition)

        // Assert
        assertThat(actual?.slides?.map(StoryV2Slide::id)).containsExactly("good")
    }

    @Test
    fun `GIVEN no showable slide WHEN validated THEN the story is refused`() {
        // Arrange
        val composition = composition(slides = listOf(videoSlide(id = "bad", durationMs = 0L)))

        // Act
        val actual = StoryV2CompositionValidator.validate(composition)

        // Assert — the host has to continue its flow rather than open a story with nothing in it.
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN more than two actions WHEN validated THEN one per style survives`() {
        // Arrange
        val composition = composition(
            slides = listOf(videoSlide(id = "slide")),
            actions = listOf(
                action(id = "first_primary", style = StoryV2Action.Style.PRIMARY),
                action(id = "second_primary", style = StoryV2Action.Style.PRIMARY),
                action(id = "secondary", style = StoryV2Action.Style.SECONDARY),
            ),
        )

        // Act
        val actual = StoryV2CompositionValidator.validate(composition)

        // Assert
        assertThat(actual?.actions?.map(StoryV2Action::id)).containsExactly("first_primary", "secondary").inOrder()
    }

    @Test
    fun `GIVEN slide actions WHEN validated THEN they are sanitized like the story ones`() {
        // Arrange
        val slideActions = listOf(
            action(id = "a", style = StoryV2Action.Style.PRIMARY),
            action(id = "b", style = StoryV2Action.Style.PRIMARY),
        )
        val composition = composition(slides = listOf(videoSlide(id = "slide").copy(actions = slideActions)))

        // Act
        val actual = StoryV2CompositionValidator.validate(composition)

        // Assert
        assertThat(actual?.slides?.first()?.actions?.map(StoryV2Action::id)).containsExactly("a")
    }

    @Test
    fun `GIVEN an empty slide action list WHEN validated THEN it stays empty rather than inheriting`() {
        // Arrange
        val composition = composition(
            slides = listOf(videoSlide(id = "slide").copy(actions = emptyList())),
            actions = listOf(action(id = "story", style = StoryV2Action.Style.PRIMARY)),
        )

        // Act
        val actual = StoryV2CompositionValidator.validate(composition)

        // Assert — an empty list hides the actions on that slide; only an absent list inherits.
        assertThat(actual?.actionsOf(actual.slides.first())).isEmpty()
    }

    @Test
    fun `GIVEN haptic markers out of range or repeated WHEN validated THEN only reachable unique ones remain`() {
        // Arrange
        val markers = listOf(2_000L, -1L, 2_000L, 5_000L, 6_000L, 1_000L)
        val composition = composition(
            slides = listOf(videoSlide(id = "slide", durationMs = 5_000L).copy(hapticAtMs = markers)),
        )

        // Act
        val actual = StoryV2CompositionValidator.validate(composition)

        // Assert — a marker at or past the duration is never reached, and a duplicate would fire twice on one frame.
        assertThat(actual?.slides?.first()?.hapticAtMs).containsExactly(1_000L, 2_000L).inOrder()
    }

    @Test
    fun `GIVEN haptic markers on an image slide WHEN validated THEN they are removed`() {
        // Arrange
        val slide = StoryV2Slide(
            id = "image",
            title = null,
            subtitle = null,
            asset = StoryV2Asset.Image(
                source = StoryV2MediaRef.DrawableResource(id = 1),
                durationMs = 4_000L,
            ),
            hapticAtMs = listOf(1_000L),
        )

        // Act
        val actual = StoryV2CompositionValidator.validate(composition(slides = listOf(slide)))

        // Assert — an image has no timeline to hang a marker on.
        assertThat(actual?.slides?.first()?.hapticAtMs).isEmpty()
    }

    private fun composition(slides: List<StoryV2Slide>, actions: List<StoryV2Action> = emptyList()) =
        StoryV2Composition(
            id = "story",
            version = 1,
            origin = StoryV2Origin.BUNDLED,
            endBehavior = StoryV2EndBehavior.LOOP,
            actions = actions,
            slides = slides,
        )

    private fun videoSlide(id: String, durationMs: Long = 5_000L) = StoryV2Slide(
        id = id,
        title = stringReference(value = id),
        subtitle = null,
        asset = StoryV2Asset.Video(
            source = StoryV2MediaRef.RawResource(id = 1),
            poster = StoryV2MediaRef.DrawableResource(id = 2),
            durationMs = durationMs,
        ),
    )

    private fun action(id: String, style: StoryV2Action.Style) = StoryV2Action(
        id = id,
        label = stringReference(value = id),
        style = style,
        target = StoryV2Action.Target.Close,
    )
}