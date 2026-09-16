package com.tangem.features.txhistory.joint.operation.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.domain.jointaccount.model.JointOperationSignError.VerificationFailed.Reason
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreUiR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointOperationErrorMessageTest {

    private var supportClicks = 0
    private var operationGone = 0

    private val converter = JointOperationErrorMessage(
        onSupportClick = { supportClicks++ },
        onOperationGone = { operationGone++ },
    )

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = converter.convert(model.error)

        // Assert
        assertThat(actual?.title?.resId()).isEqualTo(model.title)
        assertThat(actual?.message?.resId()).isEqualTo(model.message)
        assertThat(actual?.secondAction != null).isEqualTo(model.hasSecondAction)
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            error = JointOperationSignError.OperationNotFound,
            title = CoreUiR.string.joint_operation_completed_title,
            message = CoreUiR.string.joint_operation_completed_subtitle,
        ),
        ConvertModel(
            error = JointOperationSignError.ThresholdReached,
            title = CoreUiR.string.joint_operation_threshold_reached_title,
            message = CoreUiR.string.joint_operation_threshold_reached_subtitle,
        ),
        ConvertModel(
            error = JointOperationSignError.Malicious,
            title = CoreUiR.string.joint_operation_malicious_title,
            message = CoreUiR.string.joint_operation_malicious_subtitle,
        ),
        ConvertModel(
            error = JointOperationSignError.VerificationFailed(reason = Reason.REFUND_MISMATCH),
            title = CoreUiR.string.joint_operation_signing_blocked_title,
            message = CoreUiR.string.joint_operation_signing_blocked_subtitle,
            hasSecondAction = true,
        ),
        ConvertModel(
            error = JointOperationSignError.VerificationFailed(reason = Reason.OWNER_KEY_NOT_DERIVED),
            title = CoreUiR.string.common_error,
            message = CoreUiR.string.common_something_went_wrong,
        ),
        ConvertModel(
            error = JointOperationSignError.Failed(cause = IllegalStateException()),
            title = CoreUiR.string.common_error,
            message = CoreUiR.string.common_something_went_wrong,
        ),
        ConvertModel(error = JointOperationSignError.UserCancelled, title = null, message = null),
    )

    @Test
    fun `GIVEN the operation is gone WHEN the dialog is dismissed THEN the sheet is told to go`() {
        // Arrange
        val dialog = converter.convert(JointOperationSignError.OperationNotFound)

        // Act
        dialog?.onDismissRequest?.invoke()

        // Assert
        assertThat(operationGone).isEqualTo(1)
    }

    @Test
    fun `GIVEN a security refusal WHEN support is tapped THEN the support request opens`() {
        // Arrange
        val dialog = converter.convert(JointOperationSignError.VerificationFailed(reason = Reason.NOT_A_MEMBER))

        // Act
        dialog?.firstAction?.onClick?.invoke()

        // Assert
        assertThat(supportClicks).isEqualTo(1)
    }

    internal data class ConvertModel(
        val error: JointOperationSignError,
        val title: Int?,
        val message: Int?,
        val hasSecondAction: Boolean = false,
    )

    private fun TextReference.resId(): Int? = (this as? TextReference.Res)?.id
}