package com.tangem.features.jointaccount.creation.composition.ui.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.jointaccount.impl.R
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountCompositionUMTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN members and signers WHEN accessLossNote THEN it names how many lost wallets freeze the funds`(
        model: Model,
    ) {
        // Arrange
        val state = createState(totalMembers = model.total, requiredToSign = model.required)

        // Act
        val actual = state.accessLossNote

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        Model(total = 2, required = 1, expected = all),
        Model(total = 5, required = 1, expected = all),
        Model(total = 2, required = 2, expected = any),
        Model(total = 5, required = 5, expected = any),
        Model(total = 3, required = 2, expected = some(lost = "2", total = "3")),
        Model(total = 5, required = 2, expected = some(lost = "4", total = "5")),
        Model(total = 5, required = 3, expected = some(lost = "3", total = "5")),
        Model(total = 5, required = 4, expected = some(lost = "2", total = "5")),
    )

    private val all = resourceReference(R.string.joint_account_members_access_loss_all)
    private val any = resourceReference(R.string.joint_account_members_access_loss_any)

    private fun some(lost: String, total: String) = resourceReference(
        id = R.string.joint_account_members_access_loss_some,
        formatArgs = wrappedList(lost, total),
    )

    private fun createState(totalMembers: Int, requiredToSign: Int) = JointAccountCompositionUM(
        totalMembers = createStepper(value = totalMembers),
        requiredToSign = createStepper(value = requiredToSign),
        maxMembers = 5,
        onContinueClick = {},
        onBackClick = {},
    )

    private fun createStepper(value: Int) = JointAccountCompositionUM.StepperUM(
        value = value,
        isDecrementEnabled = true,
        isIncrementEnabled = true,
        onDecrement = {},
        onIncrement = {},
    )

    data class Model(val total: Int, val required: Int, val expected: TextReference)
}