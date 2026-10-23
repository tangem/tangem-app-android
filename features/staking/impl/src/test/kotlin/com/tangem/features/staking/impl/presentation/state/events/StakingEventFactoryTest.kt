package com.tangem.features.staking.impl.presentation.state.events

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.staking.impl.R
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class StakingEventFactoryTest {

    private val messageSender: UiMessageSender = mockk()
    private val popBackStack: () -> Unit = mockk(relaxed = true)
    private val onFailedTxEmailClick: (String) -> Unit = mockk(relaxed = true)
    private val dialogFactory: TransactionErrorDialogFactory = mockk()
    private val sent = mutableListOf<UiMessage>()

    private val factory = StakingEventFactory(
        messageSender = messageSender,
        popBackStack = popBackStack,
        onFailedTxEmailClick = onFailedTxEmailClick,
        transactionErrorDialogFactory = dialogFactory,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(messageSender, popBackStack, onFailedTxEmailClick, dialogFactory)
        sent.clear()
        every { messageSender.send(capture(sent)) } just Runs
    }

    @Test
    fun `GIVEN error text WHEN createGenericErrorAlert THEN unknown error dialog with support action`() {
        // Act
        factory.createGenericErrorAlert("err")
        dialog().firstAction.onClick()

        // Assert
        assertThat(dialog().title).isEqualTo(resourceReference(R.string.common_error))
        assertThat(dialog().message).isEqualTo(resourceReference(R.string.common_unknown_error))
        assertThat(dialog().firstAction.title).isEqualTo(resourceReference(R.string.common_support))
        verify(exactly = 1) { onFailedTxEmailClick("err") }
    }

    @Test
    fun `GIVEN null error WHEN createSendTransactionErrorAlert THEN nothing sent`() {
        // Act
        factory.createSendTransactionErrorAlert(null)

        // Assert
        assertThat(sent).isEmpty()
        verify(exactly = 0) { dialogFactory.create(any(), any(), any()) }
    }

    @Test
    fun `GIVEN dialog produced WHEN createSendTransactionErrorAlert THEN dialog sent`() {
        // Arrange
        val expected = DialogMessage(message = stringReference("m"))
        every {
            dialogFactory.create(SendTransactionError.DemoCardError, popBackStack, onFailedTxEmailClick)
        } returns expected

        // Act
        factory.createSendTransactionErrorAlert(SendTransactionError.DemoCardError)

        // Assert
        assertThat(sent.single()).isSameInstanceAs(expected)
    }

    @Test
    fun `GIVEN factory returns null WHEN createSendTransactionErrorAlert THEN nothing sent`() {
        // Arrange
        every { dialogFactory.create(SendTransactionError.UserCancelledError, any(), any()) } returns null

        // Act
        factory.createSendTransactionErrorAlert(SendTransactionError.UserCancelledError)

        // Assert
        assertThat(sent).isEmpty()
    }

    @Test
    fun `GIVEN staking error WHEN createStakingErrorAlert THEN code dialog with support action`() {
        // Arrange
        val error = StakingError.DomainError("x")

        // Act
        factory.createStakingErrorAlert(error)
        dialog().firstAction.onClick()

        // Assert
        assertThat(dialog().title).isEqualTo(resourceReference(R.string.common_error))
        assertThat(dialog().message)
            .isEqualTo(resourceReference(R.string.generic_error_code, wrappedList(error.toString())))
        verify(exactly = 1) { onFailedTxEmailClick(error.toString()) }
    }

    private fun dialog() = sent.single() as DialogMessage
}