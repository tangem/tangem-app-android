package com.tangem.common.ui.backup

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.status.usecase.GetBackupProblematicWalletForAddressUseCase
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.feedback.SendBackupProblemEmailUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
internal class BackupErrorWarningTest {

    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase = mockk()
    private val getBackupProblematicWalletForAddressUseCase: GetBackupProblematicWalletForAddressUseCase = mockk()
    private val sendBackupProblemEmailUseCase: SendBackupProblemEmailUseCase = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)

    private val warning = BackupErrorWarning(
        messageSender = messageSender,
        isWalletBackupProblematicUseCase = isWalletBackupProblematicUseCase,
        getBackupProblematicWalletForAddressUseCase = getBackupProblematicWalletForAddressUseCase,
        sendBackupProblemEmailUseCase = sendBackupProblemEmailUseCase,
    )

    private val problematicWalletId = UserWalletId("1234567890ABCDEF")
    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns problematicWalletId
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            isWalletBackupProblematicUseCase,
            getBackupProblematicWalletForAddressUseCase,
            sendBackupProblemEmailUseCase,
            messageSender,
            answers = false,
        )
    }

    private fun capturedDialog(): DialogMessage {
        val slot = slot<DialogMessage>()
        verify(exactly = 1) { messageSender.send(capture(slot)) }
        return slot.captured
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ForWallet {

        @Test
        fun `GIVEN backup is fine WHEN forWallet THEN proceed without a warning`() = runTest {
            // Arrange
            every { isWalletBackupProblematicUseCase(userWallet) } returns false
            var proceeded = false

            // Act
            warning.forWallet(scope = this, userWallet = userWallet) { proceeded = true }

            // Assert
            assertThat(proceeded).isTrue()
            verify(exactly = 0) { messageSender.send(any()) }
        }

        @Test
        fun `GIVEN problematic backup WHEN forWallet THEN warn and proceed only on continue`() = runTest {
            // Arrange
            every { isWalletBackupProblematicUseCase(userWallet) } returns true
            var proceeded = false
            var warningShown = false

            // Act
            warning.forWallet(
                scope = this,
                userWallet = userWallet,
                onWarningShown = { warningShown = true },
                onProceed = { proceeded = true },
            )

            // Assert
            assertThat(warningShown).isTrue()
            assertThat(proceeded).isFalse()
            capturedDialog().firstAction.onClick()
            assertThat(proceeded).isTrue()
        }

        @Test
        fun `GIVEN problematic backup WHEN contact support clicked THEN send email and do not proceed`() = runTest {
            // Arrange
            every { isWalletBackupProblematicUseCase(userWallet) } returns true
            var proceeded = false
            var supportClicked = false

            // Act
            warning.forWallet(
                scope = this,
                userWallet = userWallet,
                onSupportClick = { supportClicked = true },
                onProceed = { proceeded = true },
            )
            capturedDialog().secondAction?.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(supportClicked).isTrue()
            assertThat(proceeded).isFalse()
            coVerify(exactly = 1) { sendBackupProblemEmailUseCase(problematicWalletId) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ForAddress {

        private var currentAddress = ""

        @Test
        fun `GIVEN blank address WHEN forAddress THEN proceed without a lookup`() = runTest {
            // Arrange
            var proceeded = false

            // Act
            warning.forAddress(scope = this, address = { "" }) { proceeded = true }
            advanceUntilIdle()

            // Assert
            assertThat(proceeded).isTrue()
            coVerify(exactly = 0) { getBackupProblematicWalletForAddressUseCase(any()) }
            verify(exactly = 0) { messageSender.send(any()) }
        }

        @Test
        fun `GIVEN address of a healthy wallet WHEN forAddress THEN proceed without a warning`() = runTest {
            // Arrange
            coEvery { getBackupProblematicWalletForAddressUseCase("addr") } returns null
            var proceeded = false

            // Act
            warning.forAddress(scope = this, address = { "addr" }) { proceeded = true }
            advanceUntilIdle()

            // Assert
            assertThat(proceeded).isTrue()
            verify(exactly = 0) { messageSender.send(any()) }
        }

        @Test
        fun `GIVEN problematic address WHEN continue clicked THEN proceed`() = runTest {
            // Arrange
            coEvery { getBackupProblematicWalletForAddressUseCase("addr") } returns problematicWalletId
            var proceeded = false

            // Act
            warning.forAddress(scope = this, address = { "addr" }) { proceeded = true }
            advanceUntilIdle()

            // Assert
            assertThat(proceeded).isFalse()
            capturedDialog().firstAction.onClick()
            assertThat(proceeded).isTrue()
        }

        @Test
        fun `GIVEN healthy address edited to a problematic one during the check THEN warn instead of proceeding`() =
            runTest {
                // Arrange
                coEvery { getBackupProblematicWalletForAddressUseCase("healthy") } coAnswers {
                    currentAddress = "problematic"
                    null
                }
                coEvery { getBackupProblematicWalletForAddressUseCase("problematic") } returns problematicWalletId
                currentAddress = "healthy"
                var proceeded = false

                // Act
                warning.forAddress(scope = this, address = { currentAddress }) { proceeded = true }
                advanceUntilIdle()

                // Assert
                assertThat(proceeded).isFalse()
                capturedDialog().firstAction.onClick()
                assertThat(proceeded).isTrue()
            }

        @Test
        fun `GIVEN address edited during the check WHEN continue clicked THEN re-check the new address`() = runTest {
            // Arrange
            coEvery { getBackupProblematicWalletForAddressUseCase("problematic") } returns problematicWalletId
            coEvery { getBackupProblematicWalletForAddressUseCase("healthy") } returns null
            currentAddress = "problematic"
            var proceeded = false

            // Act
            warning.forAddress(scope = this, address = { currentAddress }) { proceeded = true }
            advanceUntilIdle()
            currentAddress = "healthy"
            capturedDialog().firstAction.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(proceeded).isTrue()
            coVerify(exactly = 1) { getBackupProblematicWalletForAddressUseCase("healthy") }
        }
    }
}