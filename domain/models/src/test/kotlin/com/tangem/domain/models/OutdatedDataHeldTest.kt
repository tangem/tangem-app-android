package com.tangem.domain.models

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OutdatedDataHeldTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isOutdatedDataHeld(model: HeldModel) {
        // Act
        val actual = isOutdatedDataHeld(model.isOutdated, model.source, model.wasHeld)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class HeldModel(
        val isOutdated: Boolean,
        val source: StatusSource?,
        val wasHeld: Boolean,
        val expected: Boolean,
    )

    private fun provideTestModels() = listOf(
        HeldModel(isOutdated = true, source = StatusSource.ONLY_CACHE, wasHeld = false, expected = true),
        HeldModel(isOutdated = true, source = StatusSource.CACHE, wasHeld = false, expected = true),
        HeldModel(isOutdated = false, source = StatusSource.CACHE, wasHeld = true, expected = true),
        HeldModel(isOutdated = false, source = StatusSource.CACHE, wasHeld = false, expected = false),
        HeldModel(isOutdated = false, source = StatusSource.ACTUAL, wasHeld = true, expected = false),
        HeldModel(isOutdated = false, source = null, wasHeld = true, expected = true),
        HeldModel(isOutdated = false, source = null, wasHeld = false, expected = false),
    )
}