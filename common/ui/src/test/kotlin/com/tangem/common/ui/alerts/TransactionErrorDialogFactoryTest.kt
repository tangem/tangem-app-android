package com.tangem.common.ui.alerts

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TransactionErrorDialogFactoryTest {

    private val popBackStack: () -> Unit = mockk(relaxed = true)
    private val onFailedTxEmailClick: (String) -> Unit = mockk(relaxed = true)
    private val onCancel: () -> Unit = mockk(relaxed = true)

    private val factory = TransactionErrorDialogFactory()

    @BeforeEach
    fun resetMocks() {
        clearMocks(popBackStack, onFailedTxEmailClick, onCancel)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an error dialog WHEN Close is clicked THEN onCancel is called only for the hooked error types`(
        model: CloseModel,
    ) {
        // Arrange
        val dialog = factory.create(
            error = model.error,
            popBackStack = popBackStack,
            onFailedTxEmailClick = onFailedTxEmailClick,
            onCancel = onCancel,
        )

        // Act
        dialog?.secondAction?.onClick?.invoke()

        // Assert
        verify(exactly = model.expectedCancelCalls) { onCancel() }
        verify(exactly = 0) { onFailedTxEmailClick(any()) }
        verify(exactly = 0) { popBackStack() }
    }

    @Test
    fun `GIVEN a Tangem SDK error WHEN Support is clicked THEN onCancel is not called`() {
        // Arrange
        val dialog = factory.create(
            error = TANGEM_SDK_ERROR,
            popBackStack = popBackStack,
            onFailedTxEmailClick = onFailedTxEmailClick,
            onCancel = onCancel,
        )

        // Act
        dialog?.firstAction?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { onFailedTxEmailClick("42") }
        verify(exactly = 0) { onCancel() }
    }

    @Test
    fun `GIVEN no onCancel WHEN Close is clicked THEN nothing fails`() {
        // Arrange
        val dialog = factory.create(
            error = TANGEM_SDK_ERROR,
            popBackStack = popBackStack,
            onFailedTxEmailClick = onFailedTxEmailClick,
        )

        // Act
        val result = runCatching { dialog?.secondAction?.onClick?.invoke() }

        // Assert
        assertThat(dialog?.secondAction).isNotNull()
        assertThat(result.exceptionOrNull()).isNull()
        verify(exactly = 0) { onFailedTxEmailClick(any()) }
        verify(exactly = 0) { popBackStack() }
    }

    @Test
    fun `GIVEN a demo card error WHEN created THEN the dialog has no second action`() {
        // Act
        val dialog = factory.create(
            error = SendTransactionError.DemoCardError,
            popBackStack = popBackStack,
            onFailedTxEmailClick = onFailedTxEmailClick,
            onCancel = onCancel,
        )

        // Assert
        assertThat(dialog).isNotNull()
        assertThat(dialog?.secondAction).isNull()
        verify(exactly = 0) { onCancel() }
    }

    private fun provideTestModels() = listOf(
        CloseModel(error = TANGEM_SDK_ERROR, expectedCancelCalls = 1),
        CloseModel(
            error = SendTransactionError.BlockchainSdkError(code = 7, message = "failed"),
            expectedCancelCalls = 1,
        ),
        CloseModel(error = SendTransactionError.DataError(message = "no data"), expectedCancelCalls = 1),
        CloseModel(
            error = SendTransactionError.NetworkError(message = "timeout", code = "504"),
            expectedCancelCalls = 1,
        ),
        CloseModel(error = SendTransactionError.UnknownError(), expectedCancelCalls = 1),
        CloseModel(error = SendTransactionError.CreateAccountUnderfunded(amount = "1 DOT"), expectedCancelCalls = 0),
        CloseModel(error = SendTransactionError.GaslessFeeIncreased, expectedCancelCalls = 0),
    )

    internal data class CloseModel(val error: SendTransactionError, val expectedCancelCalls: Int) {
        override fun toString(): String = error::class.simpleName.orEmpty()
    }

    private companion object {
        val TANGEM_SDK_ERROR = SendTransactionError.TangemSdkError(code = 42, messageRes = 0, args = emptyList())
    }
}