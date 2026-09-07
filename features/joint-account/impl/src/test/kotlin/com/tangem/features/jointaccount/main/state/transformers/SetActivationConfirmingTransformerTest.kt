package com.tangem.features.jointaccount.main.state.transformers

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SetActivationConfirmingTransformerTest {

    @Test
    fun `GIVEN sheet shown WHEN confirming starts THEN the confirm button shows a loader`() {
        // Arrange
        val prevState = createMembersUM(
            activation = createActivation(confirmation = createConfirmation(isConfirming = false)),
        )

        // Act
        val actual = SetActivationConfirmingTransformer(isConfirming = true).transform(prevState)

        // Assert
        assertThat(actual.activation?.confirmation?.isConfirming).isTrue()
    }

    @Test
    fun `GIVEN loader shown WHEN confirming ends THEN the loader is gone`() {
        // Arrange
        val prevState = createMembersUM(
            activation = createActivation(confirmation = createConfirmation(isConfirming = true)),
        )

        // Act
        val actual = SetActivationConfirmingTransformer(isConfirming = false).transform(prevState)

        // Assert
        assertThat(actual.activation?.confirmation?.isConfirming).isFalse()
    }

    @Test
    fun `GIVEN no sheet WHEN confirming starts THEN state is unchanged`() {
        // Arrange
        val prevState = createMembersUM(activation = createActivation(confirmation = null))

        // Act
        val actual = SetActivationConfirmingTransformer(isConfirming = true).transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(prevState)
    }

    @Test
    fun `GIVEN no activation WHEN confirming starts THEN state is unchanged`() {
        // Arrange
        val prevState = createMembersUM(activation = null)

        // Act
        val actual = SetActivationConfirmingTransformer(isConfirming = true).transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(prevState)
    }
}