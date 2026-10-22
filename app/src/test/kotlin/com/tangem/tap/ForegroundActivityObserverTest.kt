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

    private fun createActivity(isDestroyed: Boolean): AppCompatActivity = mockk {
        every { this@mockk.isDestroyed } returns isDestroyed
    }
}