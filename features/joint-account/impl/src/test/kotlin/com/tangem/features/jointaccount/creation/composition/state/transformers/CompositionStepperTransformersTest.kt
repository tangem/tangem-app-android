package com.tangem.features.jointaccount.creation.composition.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM
import com.tangem.features.jointaccount.creation.model.JointAccountCreationDraft
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.transformer.Transformer
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CompositionStepperTransformersTest {

    @ParameterizedTest
    @ProvideTestModels
    fun transform(model: TransformModel) {
        // Arrange
        val prevState = createState(totalMembers = model.total, requiredToSign = model.required)

        // Act
        val actual = model.transformer.transform(prevState)

        // Assert
        val expected = createState(totalMembers = model.expectedTotal, requiredToSign = model.expectedRequired)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        // total members steps up within 2..5
        TransformModel(
            transformer = ChangeTotalMembersTransformer(delta = 1),
            total = 2, required = 2,
            expectedTotal = 3, expectedRequired = 2,
        ),
        // total members does not go above 5
        TransformModel(
            transformer = ChangeTotalMembersTransformer(delta = 1),
            total = 5, required = 3,
            expectedTotal = 5, expectedRequired = 3,
        ),
        // total members does not go below 2
        TransformModel(
            transformer = ChangeTotalMembersTransformer(delta = -1),
            total = 2, required = 2,
            expectedTotal = 2, expectedRequired = 2,
        ),
        // lowering the total clamps "required to sign" to the new upper bound
        TransformModel(
            transformer = ChangeTotalMembersTransformer(delta = -1),
            total = 3, required = 3,
            expectedTotal = 2, expectedRequired = 2,
        ),
        // lowering the total leaves an in-range "required to sign" untouched
        TransformModel(
            transformer = ChangeTotalMembersTransformer(delta = -1),
            total = 5, required = 2,
            expectedTotal = 4, expectedRequired = 2,
        ),
        // required to sign steps down to 1 at minimum
        TransformModel(
            transformer = ChangeRequiredToSignTransformer(delta = -1),
            total = 2, required = 1,
            expectedTotal = 2, expectedRequired = 1,
        ),
        // required to sign steps up but never above the total
        TransformModel(
            transformer = ChangeRequiredToSignTransformer(delta = 1),
            total = 3, required = 3,
            expectedTotal = 3, expectedRequired = 3,
        ),
        // required to sign steps within 1..total
        TransformModel(
            transformer = ChangeRequiredToSignTransformer(delta = 1),
            total = 3, required = 1,
            expectedTotal = 3, expectedRequired = 2,
        ),
        // restoring the draft applies the saved values and re-derives the −/+ flags
        TransformModel(
            transformer = RestoreCompositionDraftTransformer(
                draft = JointAccountCreationDraft.Composition(totalMembers = 4, requiredToSign = 3),
            ),
            total = 2, required = 2,
            expectedTotal = 4, expectedRequired = 3,
        ),
        // restoring a draft at the upper bounds disables both + buttons
        TransformModel(
            transformer = RestoreCompositionDraftTransformer(
                draft = JointAccountCreationDraft.Composition(totalMembers = 5, requiredToSign = 5),
            ),
            total = 2, required = 2,
            expectedTotal = 5, expectedRequired = 5,
        ),
    )

    internal data class TransformModel(
        val transformer: Transformer<JointAccountCompositionUM>,
        val total: Int,
        val required: Int,
        val expectedTotal: Int,
        val expectedRequired: Int,
    )

    /** Builds the state with the −/+ flags the transformers are expected to derive for the given values */
    private fun createState(totalMembers: Int, requiredToSign: Int): JointAccountCompositionUM {
        return JointAccountCompositionUM(
            totalMembers = JointAccountCompositionUM.StepperUM(
                value = totalMembers,
                isDecrementEnabled = totalMembers > JointAccountCompositionUM.MIN_MEMBERS,
                isIncrementEnabled = totalMembers < JointAccountCompositionUM.MAX_MEMBERS,
                onDecrement = onDecrementTotal,
                onIncrement = onIncrementTotal,
            ),
            requiredToSign = JointAccountCompositionUM.StepperUM(
                value = requiredToSign,
                isDecrementEnabled = requiredToSign > JointAccountCompositionUM.MIN_REQUIRED_TO_SIGN,
                isIncrementEnabled = requiredToSign < totalMembers,
                onDecrement = onDecrementRequired,
                onIncrement = onIncrementRequired,
            ),
            maxMembers = JointAccountCompositionUM.MAX_MEMBERS,
            onContinueClick = onContinue,
            onBackClick = onBack,
        )
    }

    private companion object {
        // Shared instances so states built before and after a transform compare equal
        val onDecrementTotal: () -> Unit = {}
        val onIncrementTotal: () -> Unit = {}
        val onDecrementRequired: () -> Unit = {}
        val onIncrementRequired: () -> Unit = {}
        val onContinue: () -> Unit = {}
        val onBack: () -> Unit = {}
    }
}