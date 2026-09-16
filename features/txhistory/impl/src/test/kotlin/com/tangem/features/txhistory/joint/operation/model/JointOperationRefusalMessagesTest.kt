package com.tangem.features.txhistory.joint.operation.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreUiR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationRefusalMessagesTest {

    private var confirmed = false
    private val messages = JointOperationRefusalMessages(onConfirm = { confirmed = true })

    @Test
    fun `GIVEN the initiator WHEN confirmation THEN it asks to delete and the warning button confirms`() {
        // Arrange
        confirmed = false

        // Act
        val actual = messages.confirmation(isInitiator = true)
        actual.firstAction.onClick()

        // Assert
        assertThat(actual.title).isEqualTo(res(CoreUiR.string.joint_operation_delete_alert_title))
        assertThat(actual.firstAction.title).isEqualTo(res(CoreUiR.string.common_delete))
        assertThat(actual.firstAction.isWarning).isTrue()
        assertThat(actual.secondAction?.title).isEqualTo(cancel())
        assertThat(confirmed).isTrue()
    }

    @Test
    fun `GIVEN a co-signer WHEN confirmation THEN it asks to decline and cancel confirms nothing`() {
        // Arrange
        confirmed = false

        // Act
        val actual = messages.confirmation(isInitiator = false)
        actual.secondAction?.onClick?.invoke()

        // Assert
        assertThat(actual.title).isEqualTo(res(CoreUiR.string.joint_operation_decline_alert_title))
        assertThat(actual.firstAction.title).isEqualTo(res(CoreUiR.string.common_decline))
        assertThat(confirmed).isFalse()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun error(model: ErrorModel) {
        // Act
        val actual = messages.error(model.error)

        // Assert
        assertThat(actual?.title).isEqualTo(model.expectedTitle)
    }

    private fun provideTestModels() = listOf(
        ErrorModel(error = JointOperationRejectError.OperationNotFound, expectedTitle = null),
        ErrorModel(
            error = JointOperationRejectError.ThresholdReached,
            expectedTitle = stringReference("Enough signatures collected"),
        ),
        ErrorModel(error = JointOperationRejectError.NotAMember, expectedTitle = errorTitle()),
        ErrorModel(error = JointOperationRejectError.Failed(IllegalStateException()), expectedTitle = errorTitle()),
    )

    private fun cancel(): TextReference = res(CoreUiR.string.common_cancel)

    private fun errorTitle(): TextReference = res(CoreUiR.string.common_error)

    private fun res(id: Int): TextReference = resourceReference(id)

    internal data class ErrorModel(val error: JointOperationRejectError, val expectedTitle: TextReference?)
}