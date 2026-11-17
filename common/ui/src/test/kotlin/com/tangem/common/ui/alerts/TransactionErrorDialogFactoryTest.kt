package com.tangem.common.ui.alerts

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.transaction.error.SendTransactionError
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TransactionErrorDialogFactoryTest {

    private val popBackStack: () -> Unit = mockk(relaxed = true)
    private val onFailedTxEmailClick: (String) -> Unit = mockk(relaxed = true)
    private val onCancel: () -> Unit = mockk(relaxed = true)

    private val factory = TransactionErrorDialogFactory()

    @BeforeEach
    fun resetMocks() {
        clearMocks(popBackStack, onFailedTxEmailClick, onCancel)
    }

    @Test
    fun `GIVEN a Tangem SDK error WHEN Close is clicked THEN onCancel is called`() {
        // Arrange
        val dialog = factory.create(
            error = TANGEM_SDK_ERROR,
            popBackStack = popBackStack,
            onFailedTxEmailClick = onFailedTxEmailClick,
            onCancel = onCancel,
        )

        // Act
        dialog?.secondAction?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { onCancel() }
        verify(exactly = 0) { onFailedTxEmailClick(any()) }
        verify(exactly = 0) { popBackStack() }
    }

    @Test
    fun `GIVEN a blockchain SDK error WHEN Close is clicked THEN onCancel is called`() {
        // Arrange
        val dialog = factory.create(
            error = SendTransactionError.BlockchainSdkError(code = 7, message = "failed"),
            popBackStack = popBackStack,
            onFailedTxEmailClick = onFailedTxEmailClick,
            onCancel = onCancel,
        )

        // Act
        dialog?.secondAction?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { onCancel() }
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
        // Arrange & Act
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

    private companion object {
        val TANGEM_SDK_ERROR = SendTransactionError.TangemSdkError(code = 42, messageRes = 0, args = emptyList())
    }
}