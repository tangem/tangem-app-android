package com.tangem.core.res

import android.content.res.Resources
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class StringResourceErrorReporterTest {

    private val resources: Resources = mockk {
        every { getString(STRING_ID, *anyVararg()) } throws IllegalArgumentException("bad format")
        every { getResourceEntryName(STRING_ID) } returns STRING_NAME
    }

    @Test
    fun `GIVEN installed reporter WHEN string fails to format THEN failure is reported and resource name returned`() {
        // Arrange
        val reported = mutableListOf<Throwable>()
        StringResourceErrorReporter.install { reported += it }

        // Act
        val actual = resources.getStringSafe(STRING_ID, "arg")

        // Assert
        assertThat(actual).isEqualTo(STRING_NAME)
        assertThat(reported).hasSize(1)
        assertThat(reported.single()).hasMessageThat().contains("name: R.string.$STRING_NAME")
    }

    private companion object {
        const val STRING_ID = 1
        const val STRING_NAME = "broken_string"
    }
}