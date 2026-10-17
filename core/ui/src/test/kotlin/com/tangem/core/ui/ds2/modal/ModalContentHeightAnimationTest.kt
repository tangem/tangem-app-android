package com.tangem.core.ui.ds2.modal

import androidx.compose.runtime.MonotonicFrameClock
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.modal.ModalContentHeightAnimationTest.Companion.FRAME_MILLIS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ModalContentHeightAnimationTest {

    @Test
    fun `GIVEN no height followed yet WHEN heightOf THEN the measured height passes through`() {
        // Arrange
        val animation = ModalContentHeightAnimation()

        // Act
        val actual = animation.heightOf(measuredHeight = TALL_HEIGHT)

        // Assert
        assertThat(actual).isEqualTo(TALL_HEIGHT)
    }

    @Test
    fun `GIVEN the first height WHEN followed THEN it is adopted at once`() = runTest {
        // Arrange
        val animation = ModalContentHeightAnimation()

        // Act
        withFrameClock { animation.follow(height = TALL_HEIGHT) }

        // Assert
        assertThat(animation.heightOf(measuredHeight = SHORT_HEIGHT)).isEqualTo(TALL_HEIGHT)
    }

    @Test
    fun `GIVEN a settled height WHEN the content changes height THEN the card moves there gradually`() = runTest {
        // Arrange
        val animation = ModalContentHeightAnimation()
        withFrameClock { animation.follow(height = TALL_HEIGHT) }

        // Act
        val resize = launch { withFrameClock { animation.follow(height = SHORT_HEIGHT) } }
        advanceTimeBy(FRAME_MILLIS * 4)
        runCurrent()
        val midway = animation.heightOf(measuredHeight = SHORT_HEIGHT)
        resize.join()
        val settled = animation.heightOf(measuredHeight = SHORT_HEIGHT)

        // Assert
        assertThat(midway).isGreaterThan(SHORT_HEIGHT)
        assertThat(midway).isLessThan(TALL_HEIGHT)
        assertThat(settled).isEqualTo(SHORT_HEIGHT)
    }

    private suspend fun TestScope.withFrameClock(block: suspend () -> Unit) {
        withContext(TestFrameClock(scope = this)) { block() }
    }

    /** Ticks a frame every [FRAME_MILLIS] of virtual time, so springs advance under the test scheduler */
    private class TestFrameClock(private val scope: TestScope) : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(FRAME_MILLIS)
            return onFrame(scope.testScheduler.currentTime * NANOS_IN_MILLI)
        }
    }

    private companion object {
        const val TALL_HEIGHT = 1_200
        const val SHORT_HEIGHT = 400
        const val FRAME_MILLIS = 16L
        const val NANOS_IN_MILLI = 1_000_000L
    }
}