package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SwapModelSolanaGaslessSuppressionTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN new swap screen WHEN created THEN gasless is not suppressed`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.isSolanaGaslessSuppressed).isFalse()
    }
}