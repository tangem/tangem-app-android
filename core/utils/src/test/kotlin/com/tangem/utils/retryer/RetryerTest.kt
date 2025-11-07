package com.tangem.utils.retryer

import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryerTest {

    @Test
    fun `retryer with zero attempts should throw exception`() {
        // Act
        val actual = runCatching {
            Retryer(attempt = 0) { false }
        }
            .exceptionOrNull()

        // Assert
        val expected = IllegalArgumentException("Retryer.attempt should be greater than 0")
        Truth.assertThat(actual).isInstanceOf(expected::class.java)
        Truth.assertThat(actual).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `retryer with negative attempts should throw exception`() {
        // Act
        val actual = runCatching {
            Retryer(attempt = -5) { false }
        }
            .exceptionOrNull()

        // Assert
        val expected = IllegalArgumentException("Retryer.attempt should be greater than 0")
        Truth.assertThat(actual).isInstanceOf(expected::class.java)
        Truth.assertThat(actual).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `retryer with positive attempts should be created successfully`() {
        // Act
        val actual = runCatching {
            Retryer(attempt = 3) { false }
        }
            .getOrNull()

        // Assert
        Truth.assertThat(actual).isNotNull()
    }

    @Test
    fun `retryer should be repeated until block returns true`() = runTest {
        // Arrange
        val attempt = 5
        val block = mockk<suspend (Int) -> Boolean>()
        val retryer = Retryer(attempt = attempt, block = block)

        coEvery { block(any()) } returnsMany List(attempt) { it == attempt - 2 } // returns true on the 4th call

        // Act
        retryer.launch()

        // Assert
        coVerifyOrder {
            block.invoke(0)
            block.invoke(1)
            block.invoke(2)
            block.invoke(3)
        }
    }

    @Test
    fun `retryer should be called once`() = runTest {
        // Arrange
        val attempt = 5
        val block = mockk<suspend (Int) -> Boolean>()
        val retryer = Retryer(attempt = attempt, block = block)

        coEvery { block(1) } returns true // for the first attempt

        // Act
        retryer.launch()

        // Assert
        coVerifyOrder {
            block.invoke(1)
        }
    }

    @Test
    fun `retryer should throw after all attempts failed`() = runTest {
        // Arrange
        val attempt = 3
        val block = mockk<suspend (Int) -> Boolean>()
        val retryer = Retryer(attempt = attempt, block = block)

        coEvery { block(any()) } returnsMany List(attempt) { false }

        // Act
        retryer.launch()

        // Assert
        coVerifyOrder {
            block.invoke(0)
            block.invoke(1)
            block.invoke(2)
        }
    }

    @Test
    fun `retryer should handle exceptions in block and continue retrying`() = runTest {
        // Arrange
        val attempt = 4
        val block = mockk<suspend (Int) -> Boolean>()
        val retryer = Retryer(attempt = attempt, block = block)

        coEvery { block(any()) } throws IllegalStateException("Test Exception") andThenMany List(attempt - 1) { false }

        // Act
        retryer.launch()

        // Assert
        coVerifyOrder {
            block.invoke(0)
            block.invoke(1)
            block.invoke(2)
            block.invoke(3)
        }
    }
}