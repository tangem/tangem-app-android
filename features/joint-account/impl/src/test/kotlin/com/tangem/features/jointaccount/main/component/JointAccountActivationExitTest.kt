package com.tangem.features.jointaccount.main.component

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.tangem.core.ui.R as CoreUiR

internal class JointAccountActivationExitTest {

    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val closeFlow: () -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(messageSender, closeFlow)
    }

    @Test
    fun `GIVEN activation WHEN exit THEN account activated toast shown and the flow closes`() {
        // Arrange
        val exit = JointAccountActivationExit(messageSender = messageSender, closeFlow = closeFlow)

        // Act
        exit.exit()

        // Assert
        val message = slot<SnackbarMessage>()
        verify(exactly = 1) { messageSender.send(capture(message)) }
        assertThat(message.captured.message).isEqualTo(
            resourceReference(CoreUiR.string.joint_account_activated_snackbar),
        )
        verify(exactly = 1) { closeFlow() }
    }

    @Test
    fun `GIVEN activation reported by the confirm response and the account list WHEN exit twice THEN left once`() {
        // Arrange
        val exit = JointAccountActivationExit(messageSender = messageSender, closeFlow = closeFlow)

        // Act
        exit.exit()
        exit.exit()

        // Assert
        verify(exactly = 1) { messageSender.send(any()) }
        verify(exactly = 1) { closeFlow() }
    }
}