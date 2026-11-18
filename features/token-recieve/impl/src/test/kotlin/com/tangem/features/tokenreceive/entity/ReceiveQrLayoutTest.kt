package com.tangem.features.tokenreceive.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.features.tokenreceive.entity.ReceiveQrLayout.Module
import org.junit.jupiter.api.Test

internal class ReceiveQrLayoutTest {

    @Test
    fun `GIVEN all modules dark WHEN receiveQrLayout THEN finders and the logo square are left out of the dots`() {
        // Arrange
        val moduleCount = 21

        // Act
        val actual = receiveQrLayout(moduleCount = moduleCount) { _, _ -> true }

        // Assert
        val finderModules = 3 * 7 * 7
        val logoModules = 7 * 7
        assertThat(actual.dots).hasSize(moduleCount * moduleCount - finderModules - logoModules)
        assertThat(actual.dots).containsNoneOf(
            Module(row = 0, column = 0),
            Module(row = 6, column = 20),
            Module(row = 20, column = 6),
            Module(row = 7, column = 7),
            Module(row = 10, column = 10),
            Module(row = 13, column = 13),
        )
        assertThat(actual.dots).containsAtLeast(
            Module(row = 6, column = 7),
            Module(row = 14, column = 14),
            Module(row = 20, column = 20),
        )
    }

    @Test
    fun `GIVEN light modules WHEN receiveQrLayout THEN they are not drawn`() {
        // Act
        val actual = receiveQrLayout(moduleCount = 21) { row, column -> row == 20 && column == 20 }

        // Assert
        assertThat(actual.dots).containsExactly(Module(row = 20, column = 20))
    }

    @Test
    fun `GIVEN address WHEN receiveQrLayout THEN finder origins sit in three corners of the matrix`() {
        // Act
        val actual = receiveQrLayout("bc1q44yecmh2ct4u6rgqrzhqvwc636ej2r240yqts6")

        // Assert
        val lastFinderStart = actual.moduleCount - 7
        assertThat(actual.finderOrigins).containsExactly(
            Module(row = 0, column = 0),
            Module(row = 0, column = lastFinderStart),
            Module(row = lastFinderStart, column = 0),
        ).inOrder()
    }
}