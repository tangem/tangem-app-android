package com.tangem.features.tokenreceive.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import com.tangem.features.tokenreceive.entity.ReceiveQrLayout.Module
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN address WHEN receiveQrLayout THEN symbol is at least version 3`(model: ModuleCountModel) {
        // Act
        val actual = receiveQrLayout(model.address)

        // Assert
        assertThat(actual.moduleCount).isEqualTo(model.expectedModuleCount)
    }

    private fun provideTestModels() = listOf(
        ModuleCountModel(address = "0.0.30322", expectedModuleCount = 29),
        ModuleCountModel(address = "rHb9CJAWyB4rj91VRWn96DkukG4bwdtyTh", expectedModuleCount = 33),
        ModuleCountModel(address = "bc1q44yecmh2ct4u6rgqrzhqvwc636ej2r240yqts6", expectedModuleCount = 37),
        ModuleCountModel(address = "0x3987734c3379A3E2e3eF21c3C8090Cc7aBCD6260", expectedModuleCount = 37),
    )

    data class ModuleCountModel(val address: String, val expectedModuleCount: Int)
}