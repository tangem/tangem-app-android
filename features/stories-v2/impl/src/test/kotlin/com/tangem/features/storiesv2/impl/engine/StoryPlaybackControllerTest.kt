package com.tangem.features.storiesv2.impl.engine

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.features.storiesv2.StoryV2ActionTarget
import com.tangem.features.storiesv2.StoryV2Source
import com.tangem.features.storiesv2.impl.content.StoryV2Action
import com.tangem.features.storiesv2.impl.content.StoryV2Asset
import com.tangem.features.storiesv2.impl.content.StoryV2Composition
import com.tangem.features.storiesv2.impl.content.StoryV2EndBehavior
import com.tangem.features.storiesv2.impl.content.StoryV2MediaRef
import com.tangem.features.storiesv2.impl.content.StoryV2Origin
import com.tangem.features.storiesv2.impl.content.StoryV2Slide
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StoryPlaybackControllerTest {

    private val analytics: AnalyticsEventHandler = mockk()
    private val events = mutableListOf<AnalyticsEvent>()
    private val results = mutableListOf<StoriesV2Result>()
    private var hapticCount = 0

    @BeforeEach
    fun resetRecordings() {
        events.clear()
        results.clear()
        hapticCount = 0
        every { analytics.send(capture(events)) } just runs
    }

    @Test
    fun `GIVEN a story WHEN opened THEN Story Opened is sent once and the first slide is current`() {
        // Act
        val controller = openController(composition = videoStory())

        // Assert
        assertThat(events.map { it.event }).containsExactly("Story Opened")
        assertThat(events.first().params["Slides Total"]).isEqualTo("3")
        assertThat(controller.state.value.slideIndex).isEqualTo(0)
    }

    @Test
    fun `GIVEN opened twice WHEN opened again THEN Story Opened is not repeated`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        controller.onOpened()

        // Assert
        assertThat(events.count { it.event == "Story Opened" }).isEqualTo(1)
    }

    @Test
    fun `GIVEN the first slide WHEN tap back THEN the same slide restarts instead of moving`() {
        // Arrange
        val controller = openController(composition = videoStory())
        val tokenBefore = controller.state.value.playToken

        // Act
        controller.onTapBack()

        // Assert — there is no previous slide, and jumping to the last one would read as going forward.
        assertThat(controller.state.value.slideIndex).isEqualTo(0)
        assertThat(controller.state.value.playToken).isGreaterThan(tokenBefore)
    }

    @Test
    fun `GIVEN a tap WHEN a slide is opened THEN the entry is marked manual so the asset restarts`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        controller.onTapForward()

        // Assert — without this the player would resume a video it is already parked on instead of replaying it.
        assertThat(controller.state.value.isManualEntry).isTrue()
    }

    @Test
    fun `GIVEN an automatic advance WHEN a slide is opened THEN the entry is not manual`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        controller.onPlayerAdvancedTo(slideIndex = 1)

        // Assert — the player already handed over to the next item; touching it again would break the seam.
        assertThat(controller.state.value.isManualEntry).isFalse()
    }

    @Test
    fun `GIVEN the second slide WHEN tap back THEN the previous slide is opened`() {
        // Arrange
        val controller = openController(composition = videoStory())
        controller.onTapForward()

        // Act
        controller.onTapBack()

        // Assert
        assertThat(controller.state.value.slideIndex).isEqualTo(0)
        assertThat(events.last { it.event == "Slide Viewed" }.params["Advance Reason"]).isEqualTo("tap_back")
    }

    @Test
    fun `GIVEN the last slide of a finish story WHEN it ends THEN the story reports completion once`() {
        // Arrange
        val controller = openController(composition = videoStory(endBehavior = StoryV2EndBehavior.FINISH))
        controller.onTapForward()
        controller.onTapForward()

        // Act
        controller.onPlayerEnded()
        controller.onPlayerEnded()

        // Assert
        assertThat(results).containsExactly(StoriesV2Result.Completed)
        assertThat(events.count { it.event == "Story Closed" }).isEqualTo(1)
        assertThat(events.last { it.event == "Story Closed" }.params["Exit Reason"]).isEqualTo("completed")
        assertThat(events.last { it.event == "Story Closed" }.params["Story Completed"]).isEqualTo("true")
    }

    @Test
    fun `GIVEN the last slide of a loop story WHEN it ends THEN the first slide is opened and the loop counted`() {
        // Arrange
        val controller = openController(composition = videoStory())
        controller.onTapForward()
        controller.onTapForward()

        // Act
        controller.onPlayerEnded()

        // Assert
        assertThat(controller.state.value.slideIndex).isEqualTo(0)
        assertThat(events.last { it.event == "Slide Viewed" }.params["Advance Reason"]).isEqualTo("loop_restart")

        controller.onCloseClicked()
        assertThat(events.last { it.event == "Story Closed" }.params["Loops Watched"]).isEqualTo("1")
    }

    @Test
    fun `GIVEN an automatic loop round WHEN a slide is left again THEN Slide Viewed is not repeated`() {
        // Arrange — a full automatic round, then the same slides again.
        val controller = openController(composition = videoStory())
        repeat(times = 3) { controller.onPlayerEnded() }
        val afterFirstRound = events.count { it.event == "Slide Viewed" }

        // Act
        repeat(times = 3) { controller.onPlayerEnded() }

        // Assert — a story nobody touched must not report the same views twice.
        assertThat(afterFirstRound).isEqualTo(3)
        assertThat(events.count { it.event == "Slide Viewed" }).isEqualTo(3)
    }

    @Test
    fun `GIVEN a slide already viewed automatically WHEN opened by a tap THEN Slide Viewed is sent again`() {
        // Arrange
        val controller = openController(composition = videoStory())
        controller.onPlayerAdvancedTo(slideIndex = 1)
        val afterAuto = events.count { it.event == "Slide Viewed" }

        // Act — a manual return is a genuine new view of the slide.
        controller.onTapBack()
        controller.onTapForward()

        // Assert
        assertThat(afterAuto).isEqualTo(1)
        assertThat(events.count { it.event == "Slide Viewed" }).isEqualTo(3)
    }

    @Test
    fun `GIVEN a held story WHEN progress is reported THEN watched time excludes the pause`() {
        // Arrange
        val controller = openController(composition = videoStory())
        controller.onProgress(positionMs = 500L, elapsedMs = 500L)

        // Act — the frame loop reports no elapsed time while held.
        controller.setHeld(held = true)
        controller.onProgress(positionMs = 500L, elapsedMs = 0L)
        controller.setHeld(held = false)
        controller.onProgress(positionMs = 700L, elapsedMs = 200L)
        controller.onTapForward()

        // Assert
        assertThat(events.last { it.event == "Slide Viewed" }.params["Watched Ms"]).isEqualTo("700")
    }

    @Test
    fun `GIVEN haptic markers WHEN the position crosses them THEN each fires exactly once`() {
        // Arrange
        val controller = openController(composition = videoStory(hapticAtMs = listOf(1_000L, 2_000L)))

        // Act
        controller.onProgress(positionMs = 1_200L, elapsedMs = 16L)
        controller.onProgress(positionMs = 1_400L, elapsedMs = 16L)
        controller.onProgress(positionMs = 2_500L, elapsedMs = 16L)

        // Assert
        assertThat(hapticCount).isEqualTo(2)
    }

    @Test
    fun `GIVEN a held story WHEN the position crosses a marker THEN no haptic fires`() {
        // Arrange
        val controller = openController(composition = videoStory(hapticAtMs = listOf(1_000L)))

        // Act
        controller.setHeld(held = true)
        controller.onProgress(positionMs = 1_500L, elapsedMs = 0L)

        // Assert
        assertThat(hapticCount).isEqualTo(0)
    }

    @Test
    fun `GIVEN a backgrounded story WHEN the position crosses a marker THEN no haptic fires`() {
        // Arrange
        val controller = openController(composition = videoStory(hapticAtMs = listOf(1_000L)))

        // Act
        controller.setInForeground(inForeground = false)
        controller.onProgress(positionMs = 1_500L, elapsedMs = 0L)

        // Assert
        assertThat(hapticCount).isEqualTo(0)
    }

    @Test
    fun `GIVEN an image slide WHEN its duration is reached THEN it advances immediately`() {
        // Arrange
        val controller = openController(composition = imageStory())

        // Act
        controller.onProgress(positionMs = IMAGE_DURATION_MS, elapsedMs = 16L)

        // Assert — nothing else is going to report the end of a slide the composition times itself.
        assertThat(controller.state.value.slideIndex).isEqualTo(1)
    }

    @Test
    fun `GIVEN a video slide WHEN its duration is reached THEN the player is given time to advance first`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        controller.onProgress(positionMs = SLIDE_DURATION_MS, elapsedMs = 16L)
        val immediately = controller.state.value.slideIndex

        // Assert — the player normally moves on itself, which is what keeps consecutive videos gapless.
        assertThat(immediately).isEqualTo(0)

        repeat(times = 20) { controller.onProgress(positionMs = SLIDE_DURATION_MS, elapsedMs = 16L) }
        assertThat(controller.state.value.slideIndex).isEqualTo(1)
    }

    @Test
    fun `GIVEN the player advanced out of order WHEN reported THEN it is ignored`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        controller.onPlayerAdvancedTo(slideIndex = 2)

        // Assert
        assertThat(controller.state.value.slideIndex).isEqualTo(0)
    }

    @Test
    fun `GIVEN a screen action WHEN clicked THEN the target is handed to the host`() {
        // Arrange
        val action = StoryV2Action(
            id = "scan",
            label = stringReference(value = "Scan"),
            style = StoryV2Action.Style.PRIMARY,
            target = StoryV2Action.Target.Screen(key = "hardware_wallet"),
        )
        val controller = openController(composition = videoStory(actions = listOf(action)))

        // Act
        controller.onActionClicked(action)

        // Assert
        assertThat(results).containsExactly(
            StoriesV2Result.ActionInvoked(
                actionId = "scan",
                target = StoryV2ActionTarget.Screen(key = "hardware_wallet"),
            ),
        )
        val closed = events.last { it.event == "Story Closed" }
        assertThat(closed.params["Exit Reason"]).isEqualTo("action")
        assertThat(closed.params["Action Target Type"]).isEqualTo("screen")
        assertThat(closed.params["Action Destination"]).isEqualTo("hardware_wallet")
    }

    @Test
    fun `GIVEN a web action WHEN clicked THEN only the host is reported as the destination`() {
        // Arrange — a full url can carry campaign data, which has no place in an analytics property.
        val action = StoryV2Action(
            id = "learn_more",
            label = stringReference(value = "Learn more"),
            style = StoryV2Action.Style.SECONDARY,
            target = StoryV2Action.Target.Web(url = "https://buy.tangem.com/products?utm=story"),
        )
        val controller = openController(composition = videoStory(actions = listOf(action)))

        // Act
        controller.onActionClicked(action)

        // Assert
        assertThat(events.last { it.event == "Story Closed" }.params["Action Destination"])
            .isEqualTo("buy.tangem.com")
    }

    @Test
    fun `GIVEN a close action WHEN clicked THEN the story is dismissed rather than routed`() {
        // Arrange
        val action = StoryV2Action(
            id = "close",
            label = stringReference(value = "Close"),
            style = StoryV2Action.Style.SECONDARY,
            target = StoryV2Action.Target.Close,
        )
        val controller = openController(composition = videoStory(actions = listOf(action)))

        // Act
        controller.onActionClicked(action)

        // Assert
        assertThat(results).containsExactly(StoriesV2Result.Dismissed)
    }

    @Test
    fun `GIVEN a swipe down WHEN the story closes THEN the exit reason distinguishes it from the close button`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        controller.onSwipedDown()

        // Assert
        assertThat(events.last { it.event == "Story Closed" }.params["Exit Reason"]).isEqualTo("swipe_down")
    }

    @Test
    fun `GIVEN a closed story WHEN closed again THEN nothing further is reported`() {
        // Arrange
        val controller = openController(composition = videoStory())
        controller.onCloseClicked()
        val afterFirstClose = events.size

        // Act
        controller.onCloseClicked()
        controller.onTapForward()
        controller.onProgress(positionMs = SLIDE_DURATION_MS, elapsedMs = 16L)

        // Assert
        assertThat(events).hasSize(afterFirstClose)
        assertThat(results).hasSize(1)
    }

    @Test
    fun `GIVEN reduce motion WHEN a video story opens THEN the slide is shown as its poster`() {
        // Arrange
        val controller = controller(composition = videoStory())

        // Act
        controller.setReducedMotion(enabled = true)
        controller.onOpened()

        // Assert
        assertThat(controller.state.value.renderMode).isEqualTo(SlideRenderMode.POSTER)
        controller.onTapForward()
        assertThat(events.last { it.event == "Slide Viewed" }.params["Fallback Reason"]).isEqualTo("reduce_motion")
    }

    @Test
    fun `GIVEN reduce motion WHEN an image story opens THEN nothing is treated as a fallback`() {
        // Arrange
        val controller = controller(composition = imageStory())

        // Act
        controller.setReducedMotion(enabled = true)
        controller.onOpened()

        // Assert — an image has no motion to reduce.
        assertThat(controller.state.value.renderMode).isEqualTo(SlideRenderMode.ASSET)
    }

    @Test
    fun `GIVEN a stalled asset WHEN the stall outlasts the timeout THEN the slide falls back to its poster`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act — a decoder that delivers nothing reports no error, so the only signal is the clock saying the
        // position has not moved for longer than the budget.
        controller.onProgress(positionMs = 100L, elapsedMs = FRAME_MS, stalledForMs = STALL_TIMEOUT_MS + 1)

        // Assert
        assertThat(controller.state.value.renderMode).isEqualTo(SlideRenderMode.POSTER)
        val error = events.single { it.event == "Story Error Occurred" }
        assertThat(error.params["Error Stage"]).isEqualTo("asset_playback")
        assertThat(error.params["Error Reason"]).isEqualTo("timeout")

        controller.onTapForward()
        assertThat(events.last { it.event == "Slide Viewed" }.params["Fallback Reason"]).isEqualTo("asset_not_ready")
    }

    @Test
    fun `GIVEN a slide already on its poster WHEN the stall goes on THEN the failure is reported once`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act — the clock keeps reporting the stall for as long as it lasts, which is every frame.
        repeat(times = 30) {
            controller.onProgress(positionMs = 100L, elapsedMs = FRAME_MS, stalledForMs = STALL_TIMEOUT_MS + 1)
        }

        // Assert
        assertThat(events.count { it.event == "Story Error Occurred" }).isEqualTo(1)
    }

    @Test
    fun `GIVEN a slide on its poster for reduce motion WHEN playback then fails THEN the first reason is kept`() {
        // Arrange
        val controller = openController(composition = videoStory(), isReducedMotion = true)

        // Act — the silenced decoder is free to complain; that does not make this a technical fallback.
        controller.onPlaybackFailed()
        controller.onTapForward()

        // Assert
        assertThat(events.last { it.event == "Slide Viewed" }.params["Fallback Reason"]).isEqualTo("reduce_motion")
    }

    @Test
    fun `GIVEN a stall shorter than the timeout WHEN it clears THEN the slide keeps playing its asset`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act — a hiccup must not cost the viewer the video, and two hiccups either side of a recovery must not
        // add up into one timeout.
        controller.onProgress(positionMs = 100L, elapsedMs = FRAME_MS, stalledForMs = STALL_TIMEOUT_MS - 500)
        controller.onProgress(positionMs = 200L, elapsedMs = FRAME_MS, stalledForMs = 0L)
        controller.onProgress(positionMs = 300L, elapsedMs = FRAME_MS, stalledForMs = STALL_TIMEOUT_MS - 500)

        // Assert
        assertThat(controller.state.value.renderMode).isEqualTo(SlideRenderMode.ASSET)
        assertThat(events.none { it.event == "Story Error Occurred" }).isTrue()
    }

    @Test
    fun `GIVEN a held story WHEN the stall timeout would pass THEN no fallback happens`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act — a paused decoder produces nothing by design; that is not a stall. The elapsed time is deliberately
        // non-zero: the guard has to be the pause itself, not the caller having already zeroed the frame delta.
        controller.setHeld(held = true)
        repeat(times = 5) {
            controller.onProgress(positionMs = 100L, elapsedMs = FRAME_MS, stalledForMs = STALL_TIMEOUT_MS * 2)
        }

        // Assert
        assertThat(controller.state.value.renderMode).isEqualTo(SlideRenderMode.ASSET)
        assertThat(events.none { it.event == "Story Error Occurred" }).isTrue()
    }

    @Test
    fun `GIVEN a slide replayed by an automatic loop WHEN the story is closed THEN Slide Viewed is still sent`() {
        // Arrange — a full automatic round puts the story back on an already reported slide.
        val controller = openController(composition = videoStory())
        repeat(times = 3) { controller.onPlayerEnded() }
        val afterRound = events.count { it.event == "Slide Viewed" }

        // Act
        controller.onCloseClicked()

        // Assert — the slide the viewer quit on carries its watched time whatever round it was.
        assertThat(events.count { it.event == "Slide Viewed" }).isEqualTo(afterRound + 1)
        assertThat(events.last { it.event == "Slide Viewed" }.params["Advance Reason"]).isEqualTo("story_closed")
    }

    @Test
    fun `GIVEN the player advanced by itself WHEN the slide is entered THEN the entry is player driven`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        controller.onPlayerAdvancedTo(slideIndex = 1)

        // Assert — this is the only entry the renderer must leave alone.
        assertThat(controller.state.value.isPlayerDrivenEntry).isTrue()
    }

    @Test
    fun `GIVEN an automatic loop restart WHEN the first slide is entered THEN the entry is not player driven`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        repeat(times = 3) { controller.onPlayerEnded() }

        // Assert — the player is parked at the end of the run, so the asset has to be restarted explicitly.
        assertThat(controller.state.value.slideIndex).isEqualTo(0)
        assertThat(controller.state.value.isPlayerDrivenEntry).isFalse()
    }

    @Test
    fun `GIVEN the end of a slide reached by the clock WHEN the next one is entered THEN the entry is not player driven`() {
        // Arrange
        val controller = openController(composition = imageStory())

        // Act
        controller.onProgress(positionMs = IMAGE_DURATION_MS, elapsedMs = 16L)

        // Assert
        assertThat(controller.state.value.isPlayerDrivenEntry).isFalse()
    }

    @Test
    fun `GIVEN a dismiss drag WHEN it starts THEN the story pauses independently of a hold`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act — the two gestures overlap, so neither may clear the other's pause.
        controller.setHeld(held = true)
        controller.setDragging(dragging = true)
        controller.setHeld(held = false)

        // Assert
        assertThat(controller.state.value.isRunning).isFalse()

        controller.setDragging(dragging = false)
        assertThat(controller.state.value.isRunning).isTrue()
    }

    @Test
    fun `GIVEN playback failure WHEN reported THEN the slide continues on its poster and the error is recorded`() {
        // Arrange
        val controller = openController(composition = videoStory())

        // Act
        controller.onPlaybackFailed()

        // Assert
        assertThat(controller.state.value.renderMode).isEqualTo(SlideRenderMode.POSTER)
        assertThat(controller.state.value.slideIndex).isEqualTo(0)
        val error = events.last { it.event == "Story Error Occurred" }
        assertThat(error.params["Error Stage"]).isEqualTo("asset_playback")
        assertThat(error.params["Error Reason"]).isEqualTo("decode_failure")

        controller.onCloseClicked()
        assertThat(events.last { it.event == "Story Closed" }.params["Fallback Slides"]).isEqualTo("1")
    }

    @Test
    fun `GIVEN slides visited more than once WHEN the story closes THEN watched counts unique slides`() {
        // Arrange
        val controller = openController(composition = videoStory())
        controller.onTapForward()
        controller.onTapBack()
        controller.onTapForward()

        // Act
        controller.onCloseClicked()

        // Assert
        val closed = events.last { it.event == "Story Closed" }
        assertThat(closed.params["Watched"]).isEqualTo("2")
        assertThat(closed.params["Slides Total"]).isEqualTo("3")
    }

    private fun openController(
        composition: StoryV2Composition,
        isReducedMotion: Boolean = false,
    ): StoryPlaybackController {
        return controller(composition)
            .also { it.setReducedMotion(enabled = isReducedMotion) }
            .also(StoryPlaybackController::onOpened)
    }

    private fun controller(composition: StoryV2Composition) = StoryPlaybackController(
        composition = composition,
        source = StoryV2Source.ONBOARDING,
        analytics = analytics,
        onHaptic = { hapticCount++ },
        onResult = { results += it },
    )

    private fun videoStory(
        endBehavior: StoryV2EndBehavior = StoryV2EndBehavior.LOOP,
        actions: List<StoryV2Action> = emptyList(),
        hapticAtMs: List<Long> = emptyList(),
    ) = StoryV2Composition(
        id = "test_story",
        version = 7,
        origin = StoryV2Origin.BUNDLED,
        endBehavior = endBehavior,
        actions = actions,
        slides = List(size = 3) { index ->
            StoryV2Slide(
                id = "slide_$index",
                title = stringReference(value = "Title $index"),
                subtitle = null,
                asset = StoryV2Asset.Video(
                    source = StoryV2MediaRef.RawResource(id = index),
                    poster = StoryV2MediaRef.DrawableResource(id = index),
                    durationMs = SLIDE_DURATION_MS,
                ),
                hapticAtMs = hapticAtMs,
            )
        },
    )

    private fun imageStory() = StoryV2Composition(
        id = "image_story",
        version = 1,
        origin = StoryV2Origin.BUNDLED,
        endBehavior = StoryV2EndBehavior.LOOP,
        actions = emptyList(),
        slides = List(size = 2) { index ->
            StoryV2Slide(
                id = "image_$index",
                title = null,
                subtitle = null,
                asset = StoryV2Asset.Image(
                    source = StoryV2MediaRef.DrawableResource(id = index),
                    durationMs = IMAGE_DURATION_MS,
                ),
            )
        },
    )

    private companion object {
        const val SLIDE_DURATION_MS = 6_000L
        const val IMAGE_DURATION_MS = 4_000L
        const val FRAME_MS = 16L
    }
}