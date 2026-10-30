package com.tangem.tap

import androidx.appcompat.app.AppCompatActivity
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class ForegroundActivityObserverTest {

    private val observer = ForegroundActivityObserver()

    @Test
    fun `GIVEN no resumed activity WHEN foregroundActivity THEN returns null`() {
        // Act
        val actual = observer.foregroundActivity

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN resumed activity WHEN foregroundActivity THEN returns it`() {
        // Arrange
        val activity = createActivity(isDestroyed = false)
        observer.callbacks.onActivityResumed(activity)

        // Act
        val actual = observer.foregroundActivity

        // Assert
        assertThat(actual).isSameInstanceAs(activity)
    }

    @Test
    fun `GIVEN resumed activity is destroyed WHEN foregroundActivity THEN returns null`() {
        // Arrange
        val activity = createActivity(isDestroyed = false)
        observer.callbacks.onActivityResumed(activity)
        observer.callbacks.onActivityDestroyed(activity)

        // Act
        val actual = observer.foregroundActivity

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN resumed activity reports isDestroyed WHEN foregroundActivity THEN returns null`() {
        // Arrange
        val activity = createActivity(isDestroyed = true)
        observer.callbacks.onActivityResumed(activity)

        // Act
        val actual = observer.foregroundActivity

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN two resumed activities WHEN foregroundActivity THEN returns the last resumed one`() {
        // Arrange
        val first = createActivity(isDestroyed = false)
        val second = createActivity(isDestroyed = false)
        observer.callbacks.onActivityResumed(first)
        observer.callbacks.onActivityResumed(second)

        // Act
        val actual = observer.foregroundActivity

        // Assert
        assertThat(actual).isSameInstanceAs(second)
    }

    @Test
    fun `GIVEN earlier activity resumed again WHEN foregroundActivity THEN returns it`() {
        // Arrange
        val first = createActivity(isDestroyed = false)
        val second = createActivity(isDestroyed = false)
        observer.callbacks.onActivityResumed(first)
        observer.callbacks.onActivityResumed(second)
        observer.callbacks.onActivityResumed(first)

        // Act
        val actual = observer.foregroundActivity

        // Assert
        assertThat(actual).isSameInstanceAs(first)
    }

    @Test
    fun `GIVEN older instance of the same class destroyed WHEN foregroundActivity THEN returns the newer one`() {
        // Arrange
        val older = createActivity(isDestroyed = false)
        val newer = createActivity(isDestroyed = false)
        observer.callbacks.onActivityResumed(older)
        observer.callbacks.onActivityResumed(newer)
        observer.callbacks.onActivityDestroyed(older)

        // Act
        val actual = observer.foregroundActivity

        // Assert
        assertThat(actual).isSameInstanceAs(newer)
    }

    @Test
    fun `GIVEN last resumed activity destroyed WHEN foregroundActivity THEN returns the previous one`() {
        // Arrange
        val first = createActivity(isDestroyed = false)
        val second = createActivity(isDestroyed = false)
        observer.callbacks.onActivityResumed(first)
        observer.callbacks.onActivityResumed(second)
        observer.callbacks.onActivityDestroyed(second)

        // Act
        val actual = observer.foregroundActivity

        // Assert
        assertThat(actual).isSameInstanceAs(first)
    }

    private fun createActivity(isDestroyed: Boolean): AppCompatActivity = mockk {
        every { this@mockk.isDestroyed } returns isDestroyed
    }
}