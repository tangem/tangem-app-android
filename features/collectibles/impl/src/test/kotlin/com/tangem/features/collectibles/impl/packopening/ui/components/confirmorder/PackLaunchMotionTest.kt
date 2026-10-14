package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

private const val TOLERANCE = 0.001f

/** Wound up all the way, about to throw. */
private val DIPPED = ConfirmProgress(dip = 1f, launch = 0f, exit = 0f)

/** Thrown all the way: the frame before the video. */
private val LAUNCHED = ConfirmProgress(dip = 1f, launch = 1f, exit = 1f)

internal class PackLaunchMotionTest {

    @Test
    fun `GIVEN nothing tapped WHEN idle THEN the pack is untouched`() {
        // Act
        val progress = ConfirmProgress.Idle

        // Assert
        assertThat(PackLaunchMotion.offsetX(progress)).isEqualTo(0.dp)
        assertThat(PackLaunchMotion.offsetY(progress)).isEqualTo(0.dp)
        assertThat(PackLaunchMotion.scale(progress)).isWithin(TOLERANCE).of(1f)
        assertThat(PackLaunchMotion.rotationX(progress)).isWithin(TOLERANCE).of(0f)
        assertThat(PackLaunchMotion.rotationY(progress)).isWithin(TOLERANCE).of(0f)
        assertThat(PackLaunchMotion.rotationZ(progress)).isWithin(TOLERANCE).of(0f)
        assertThat(PackLaunchMotion.alpha(progress)).isWithin(TOLERANCE).of(1f)
    }

    @Test
    fun `GIVEN the wind-up WHEN it completes THEN the pack has moved down and left and leans its top in`() {
        // Act
        val x = PackLaunchMotion.offsetX(DIPPED)
        val y = PackLaunchMotion.offsetY(DIPPED)

        // Assert
        assertThat(x.value).isLessThan(0f)
        assertThat(y.value).isGreaterThan(0f)
        assertThat(PackLaunchMotion.rotationX(DIPPED)).isLessThan(0f)
    }

    /** The throw lerps from the dipped position, so a mismatch would snap the pack back. */
    @Test
    fun `GIVEN the wind-up is done WHEN the throw opens THEN it starts from where the wind-up left off`() {
        // Arrange
        val justLaunching = DIPPED.copy(launch = 0f)

        // Act & Assert
        assertThat(PackLaunchMotion.offsetX(justLaunching)).isEqualTo(PackLaunchMotion.offsetX(DIPPED))
        assertThat(PackLaunchMotion.offsetY(justLaunching)).isEqualTo(PackLaunchMotion.offsetY(DIPPED))
        assertThat(PackLaunchMotion.scale(justLaunching)).isWithin(TOLERANCE).of(PackLaunchMotion.scale(DIPPED))
        assertThat(PackLaunchMotion.rotationX(justLaunching)).isWithin(TOLERANCE).of(PackLaunchMotion.rotationX(DIPPED))
    }

    @Test
    fun `GIVEN the throw completes WHEN it ends THEN the pack has gone up and right and away into depth`() {
        // Assert
        assertThat(PackLaunchMotion.offsetX(LAUNCHED).value).isGreaterThan(0f)
        assertThat(PackLaunchMotion.offsetY(LAUNCHED).value).isLessThan(0f)
        assertThat(PackLaunchMotion.scale(LAUNCHED)).isLessThan(1f)
        assertThat(PackLaunchMotion.rotationX(LAUNCHED)).isGreaterThan(0f)
        assertThat(PackLaunchMotion.rotationY(LAUNCHED)).isGreaterThan(0f)
        assertThat(PackLaunchMotion.alpha(LAUNCHED)).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `GIVEN the throw WHEN it is not over yet THEN the pack stays solid`() {
        // Act
        val alpha = PackLaunchMotion.alpha(ConfirmProgress(dip = 1f, launch = 0.99f, exit = 0f))

        // Assert
        assertThat(alpha).isWithin(TOLERANCE).of(1f)
    }

    @Test
    fun `GIVEN the wind-up WHEN it runs THEN the pack has not begun to turn sideways`() {
        // Assert
        assertThat(PackLaunchMotion.rotationY(DIPPED)).isWithin(TOLERANCE).of(0f)
        assertThat(PackLaunchMotion.rotationZ(DIPPED)).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `GIVEN the exit WHEN it runs THEN the rows never lead the button`() {
        // Act & Assert
        listOf(0f, 0.1f, 0.3f, 0.5f, 0.9f, 1f).forEach { exit ->
            val button = PackLaunchMotion.contentShift(exit = exit, isDelayed = false)
            val rows = PackLaunchMotion.contentShift(exit = exit, isDelayed = true)

            assertThat(rows.value).isAtMost(button.value)
        }
    }

    @Test
    fun `GIVEN the exit completes WHEN it ends THEN both blocks have dropped the whole way and gone`() {
        // Act
        val button = PackLaunchMotion.contentShift(exit = 1f, isDelayed = false)
        val rows = PackLaunchMotion.contentShift(exit = 1f, isDelayed = true)

        // Assert
        assertThat(rows).isEqualTo(button)
        assertThat(PackLaunchMotion.contentAlpha(exit = 1f, isDelayed = false)).isWithin(TOLERANCE).of(0f)
        assertThat(PackLaunchMotion.contentAlpha(exit = 1f, isDelayed = true)).isWithin(TOLERANCE).of(0f)
    }
}