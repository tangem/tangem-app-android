package com.tangem.features.collectibles.impl.confirmorder.ui

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

private const val TOLERANCE = 0.001f

/** Wound up all the way, about to throw. */
private val DIPPED = ConfirmProgress(dip = 1f, launch = 0f, exit = 0f)

/** Thrown all the way: the frame before the video. */
private val LAUNCHED = ConfirmProgress(dip = 1f, launch = 1f, exit = 1f)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConfirmTransitionTest {

    @Test
    fun `GIVEN nothing tapped WHEN idle THEN the pack is untouched`() {
        // Act
        val progress = ConfirmProgress.Idle

        // Assert
        assertThat(PackLaunch.offsetX(progress)).isEqualTo(0.dp)
        assertThat(PackLaunch.offsetY(progress)).isEqualTo(0.dp)
        assertThat(PackLaunch.scale(progress)).isWithin(TOLERANCE).of(1f)
        assertThat(PackLaunch.rotation(progress)).isWithin(TOLERANCE).of(0f)
        assertThat(PackLaunch.alpha(progress)).isWithin(TOLERANCE).of(1f)
    }

    @Test
    fun `GIVEN the wind-up WHEN it completes THEN the pack has moved down and left and shrunk`() {
        // Act
        val x = PackLaunch.offsetX(DIPPED)
        val y = PackLaunch.offsetY(DIPPED)

        // Assert
        assertThat(x.value).isLessThan(0f)
        assertThat(y.value).isGreaterThan(0f)
        assertThat(PackLaunch.scale(DIPPED)).isLessThan(1f)
    }

    /** The throw lerps from the dipped position, so a mismatch would snap the pack back. */
    @Test
    fun `GIVEN the wind-up is done WHEN the throw opens THEN it starts from where the wind-up left off`() {
        // Arrange
        val justLaunching = DIPPED.copy(launch = 0f)

        // Act & Assert
        assertThat(PackLaunch.offsetX(justLaunching)).isEqualTo(PackLaunch.offsetX(DIPPED))
        assertThat(PackLaunch.offsetY(justLaunching)).isEqualTo(PackLaunch.offsetY(DIPPED))
        assertThat(PackLaunch.scale(justLaunching)).isWithin(TOLERANCE).of(PackLaunch.scale(DIPPED))
    }

    @Test
    fun `GIVEN the throw completes WHEN it ends THEN the pack has gone up and right and vanished`() {
        // Assert
        assertThat(PackLaunch.offsetX(LAUNCHED).value).isGreaterThan(0f)
        assertThat(PackLaunch.offsetY(LAUNCHED).value).isLessThan(0f)
        assertThat(PackLaunch.alpha(LAUNCHED)).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `GIVEN the wind-up WHEN it runs THEN the pack has not begun to tilt`() {
        // Assert
        assertThat(PackLaunch.rotation(DIPPED)).isWithin(TOLERANCE).of(0f)
        assertThat(PackLaunch.rotation(ConfirmProgress(dip = 0.5f, launch = 0f, exit = 0f)))
            .isWithin(TOLERANCE).of(0f)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun alpha(model: AlphaModel) {
        // Act
        val alpha = PackLaunch.alpha(ConfirmProgress(dip = 1f, launch = model.launch, exit = 0f))

        // Assert
        assertThat(alpha).isWithin(TOLERANCE).of(model.expected)
    }

    internal data class AlphaModel(val launch: Float, val expected: Float)

    private fun provideTestModels() = listOf(
        // Solid while the throw is doing the part worth watching.
        AlphaModel(launch = 0f, expected = 1f),
        AlphaModel(launch = 0.4f, expected = 1f),
        AlphaModel(launch = 0.7f, expected = 0.5f),
        AlphaModel(launch = 1f, expected = 0f),
    )

    @Test
    fun `GIVEN the exit WHEN it runs THEN the rows never lead the button`() {
        // Act & Assert
        listOf(0f, 0.1f, 0.3f, 0.5f, 0.9f, 1f).forEach { exit ->
            val button = PackLaunch.contentShift(exit = exit, isDelayed = false)
            val rows = PackLaunch.contentShift(exit = exit, isDelayed = true)

            assertThat(rows.value).isAtMost(button.value)
        }
    }

    @Test
    fun `GIVEN the exit completes WHEN it ends THEN both blocks have dropped the whole way and gone`() {
        // Act
        val button = PackLaunch.contentShift(exit = 1f, isDelayed = false)
        val rows = PackLaunch.contentShift(exit = 1f, isDelayed = true)

        // Assert
        assertThat(rows).isEqualTo(button)
        assertThat(PackLaunch.contentAlpha(exit = 1f, isDelayed = false)).isWithin(TOLERANCE).of(0f)
        assertThat(PackLaunch.contentAlpha(exit = 1f, isDelayed = true)).isWithin(TOLERANCE).of(0f)
    }
}