package com.tangem.features.jointaccount.main.state.transformers

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SetActivationConfirmationTransformerTest {

    @Test
    fun `GIVEN activation offered WHEN set confirmation THEN the sheet is shown`() {
        // Arrange
        val confirmation = createConfirmation()
        val prevState = createMembersUM(activation = createActivation(confirmation = null))

        // Act
        val actual = SetActivationConfirmationTransformer(confirmation = confirmation).transform(prevState)

        // Assert
        assertThat(actual.activation?.confirmation).isEqualTo(confirmation)
    }

    @Test
    fun `GIVEN sheet shown WHEN set null THEN the sheet is hidden`() {
        // Arrange
        val prevState = createMembersUM(activation = createActivation(confirmation = createConfirmation()))

        // Act
        val actual = SetActivationConfirmationTransformer(confirmation = null).transform(prevState)

        // Assert
        assertThat(actual.activation?.confirmation).isNull()
    }

    @Test
    fun `GIVEN no activation offered WHEN set confirmation THEN state is unchanged`() {
        // Arrange
        val prevState = createMembersUM(activation = null)

        // Act
        val actual = SetActivationConfirmationTransformer(confirmation = createConfirmation()).transform(prevState)

        // Assert
        assertThat(actual).isEqualTo(prevState)
    }
}