package com.tangem.features.storiesv2.impl.content.remote

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CopyAtMostTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a body WHEN copyAtMost THEN no more than the limit is written`(model: CopyModel) {
        // Arrange
        val input = ByteArrayInputStream(ByteArray(model.bodyBytes))
        val output = ByteArrayOutputStream()

        // Act
        input.copyAtMost(output, limit = model.limit)

        // Assert
        assertThat(output.size()).isEqualTo(model.expectedBytes)
    }

    private fun provideTestModels() = listOf(
        CopyModel(bodyBytes = 10, limit = 11, expectedBytes = 10),
        CopyModel(bodyBytes = 11, limit = 11, expectedBytes = 11),
        CopyModel(bodyBytes = 50_000, limit = 11, expectedBytes = 11),
        CopyModel(bodyBytes = 50_000, limit = 20_000, expectedBytes = 20_000),
    )

    internal data class CopyModel(val bodyBytes: Int, val limit: Long, val expectedBytes: Int)
}