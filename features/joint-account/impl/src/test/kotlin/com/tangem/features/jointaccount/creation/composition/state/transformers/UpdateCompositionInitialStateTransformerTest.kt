package com.tangem.features.jointaccount.creation.composition.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM
import org.junit.jupiter.api.Test

internal class UpdateCompositionInitialStateTransformerTest {

    private val transformer = UpdateCompositionInitialStateTransformer(
        onTotalMembersDecrement = onTotalMembersDecrement,
        onTotalMembersIncrement = onTotalMembersIncrement,
        onRequiredToSignDecrement = onRequiredToSignDecrement,
        onRequiredToSignIncrement = onRequiredToSignIncrement,
        onContinueClick = onContinueClick,
        onBackClick = onBackClick,
    )

    @Test
    fun `GIVEN empty state WHEN update initial state THEN only stepper callbacks are wired`() {
        // Act
        val actual = transformer.transform(prevState = emptyState)

        // Assert
        val expected = JointAccountCompositionUM(
            totalMembers = JointAccountCompositionUM.StepperUM(
                value = JointAccountCompositionUM.MIN_MEMBERS,
                isDecrementEnabled = false,
                isIncrementEnabled = true,
                onDecrement = onTotalMembersDecrement,
                onIncrement = onTotalMembersIncrement,
            ),
            requiredToSign = JointAccountCompositionUM.StepperUM(
                value = JointAccountCompositionUM.MIN_MEMBERS,
                isDecrementEnabled = true,
                isIncrementEnabled = false,
                onDecrement = onRequiredToSignDecrement,
                onIncrement = onRequiredToSignIncrement,
            ),
            maxMembers = JointAccountCompositionUM.MAX_MEMBERS,
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN stepped state WHEN update initial state THEN values and flags survive`() {
        // Arrange
        val steppedState = emptyState.copy(
            totalMembers = emptyState.totalMembers.copy(
                value = 4,
                isDecrementEnabled = true,
                isIncrementEnabled = true,
            ),
            requiredToSign = emptyState.requiredToSign.copy(
                value = 3,
                isDecrementEnabled = true,
                isIncrementEnabled = true,
            ),
        )

        // Act
        val actual = transformer.transform(prevState = steppedState)

        // Assert
        val expected = steppedState.copy(
            totalMembers = steppedState.totalMembers.copy(
                onDecrement = onTotalMembersDecrement,
                onIncrement = onTotalMembersIncrement,
            ),
            requiredToSign = steppedState.requiredToSign.copy(
                onDecrement = onRequiredToSignDecrement,
                onIncrement = onRequiredToSignIncrement,
            ),
            onContinueClick = onContinueClick,
            onBackClick = onBackClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    private companion object {

        /** The state the controller starts with, before the model wires the callbacks */
        val emptyState = JointAccountCompositionUM(
            totalMembers = JointAccountCompositionUM.StepperUM(
                value = JointAccountCompositionUM.MIN_MEMBERS,
                isDecrementEnabled = false,
                isIncrementEnabled = true,
                onDecrement = {},
                onIncrement = {},
            ),
            requiredToSign = JointAccountCompositionUM.StepperUM(
                value = JointAccountCompositionUM.MIN_MEMBERS,
                isDecrementEnabled = true,
                isIncrementEnabled = false,
                onDecrement = {},
                onIncrement = {},
            ),
            maxMembers = JointAccountCompositionUM.MAX_MEMBERS,
            onContinueClick = {},
            onBackClick = {},
        )

        // Shared instances so the expected state can pin the exact callbacks passed to the transformer
        val onTotalMembersDecrement: () -> Unit = {}
        val onTotalMembersIncrement: () -> Unit = {}
        val onRequiredToSignDecrement: () -> Unit = {}
        val onRequiredToSignIncrement: () -> Unit = {}
        val onContinueClick: () -> Unit = {}
        val onBackClick: () -> Unit = {}
    }
}