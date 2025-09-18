package com.tangem.utils

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValueHolderTest {

    @Test
    fun `savableContext handles empty block and returns default value`() {
        // Act
        val actual = savableContext(default = 42) {}

        // Assert
        val expected = 42
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `savableContext applies multiple updates in sequence`() {
        // Act
        val actual = savableContext(1) {
            update { it + 2 }
            update { it * 3 }
        }

        // Assert
        val expected = 9
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `ValueHolder update with function handles no-op transformation`() {
        // Act
        val holder = ValueHolder(100)
        holder.update { it }
        val actual = holder.get()

        // Assert
        val expected = 100
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `ValueHolder update with direct assignment overwrites previous value`() {
        // Act
        val holder = ValueHolder(50)
        holder.update(200)
        val actual = holder.get()

        // Assert
        val expected = 200
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `ValueHolder handles complex transformation logic`() {
        // Act
        val holder = ValueHolder(5)
        holder.update { it * it + 10 }
        val actual = holder.get()

        // Assert
        val expected = 35
        Truth.assertThat(actual).isEqualTo(expected)
    }
}