package com.tangem.utils.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class NotificationIdGeneratorTest {

    @Test
    fun `GIVEN generator WHEN next called in a burst THEN every id is unique`() {
        // Arrange
        val callsCount = 1_000

        // Act
        val ids = List(callsCount) { NotificationIdGenerator.next() }

        // Assert
        assertThat(ids.toSet()).hasSize(callsCount)
    }
}