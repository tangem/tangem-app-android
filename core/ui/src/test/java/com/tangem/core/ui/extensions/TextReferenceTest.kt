package com.tangem.core.ui.extensions

import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class TextReferenceTest {

    @Test
    fun `GIVEN array element reference WHEN resolve THEN returns element at index`() {
        // Arrange
        val resources = mockk<Resources> {
            every { getStringArray(ARRAY_ID) } returns arrayOf("first", "second", "third")
        }
        val reference = arrayItemReference(ARRAY_ID, index = 1)

        // Act
        val actual = reference.resolveReference(resources)

        // Assert
        assertThat(actual).isEqualTo("second")
    }

    @Test
    fun `GIVEN index out of bounds WHEN resolve THEN returns empty string`() {
        // Arrange
        val resources = mockk<Resources> {
            every { getStringArray(ARRAY_ID) } returns arrayOf("only")
        }
        val reference = arrayItemReference(ARRAY_ID, index = 2)

        // Act
        val actual = reference.resolveReference(resources)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN same id and index WHEN created THEN references are equal`() {
        assertThat(arrayItemReference(ARRAY_ID, index = 1))
            .isEqualTo(arrayItemReference(ARRAY_ID, index = 1))
    }

    private companion object {
        const val ARRAY_ID = 42
    }
}