package com.tangem.domain.account.usecase

import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAccountsModeEnabledUseCaseTest {

    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()

    private val useCase = IsAccountsModeEnabledUseCase(multiAccountListSupplier = multiAccountListSupplier)

    @AfterEach
    fun tearDown() {
        clearMocks(multiAccountListSupplier)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Invoke {

        @Test
        fun `returns false when supplier emits empty list`() = runTest {
            // Arrange
            every { multiAccountListSupplier.invoke() } returns flowOf(emptyList())

            // Act
            val actual = useCase.invoke().first()

            // Assert
            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when supplier emits account list with one account`() = runTest {
            // Arrange
            val accountList = createAccountList(activeAccounts = 1)
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountList))

            // Act
            val actual = useCase.invoke().first()

            // Assert
            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when supplier emits account list with two accounts`() = runTest {
            // Arrange
            val accountList = createAccountList(activeAccounts = 2)
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountList))

            // Act
            val actual = useCase.invoke().first()

            // Assert
            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when supplier emits multiple account lists, one with two accounts`() = runTest {
            // Arrange
            val accountList1 = createAccountList(activeAccounts = 1)
            val accountList2 = createAccountList(activeAccounts = 2)
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountList1, accountList2))

            // Act
            val actual = useCase.invoke().first()

            // Assert
            Truth.assertThat(actual).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeSync {

        @Test
        fun `returns false when getSyncOrNull returns null`() = runTest {
            // Arrange
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns null

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when getSyncOrNull returns empty list`() = runTest {
            // Arrange
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns emptyList()

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns false when getSyncOrNull returns account list with one account`() = runTest {
            // Arrange
            val accountList = createAccountList(activeAccounts = 1)
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(accountList)

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isFalse()
        }

        @Test
        fun `returns true when getSyncOrNull returns account list with two accounts`() = runTest {
            // Arrange
            val accountList = createAccountList(activeAccounts = 2)
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(accountList)

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isTrue()
        }

        @Test
        fun `returns true when getSyncOrNull returns multiple account lists, one with two accounts`() = runTest {
            // Arrange
            val accountList1 = createAccountList(activeAccounts = 1)
            val accountList2 = createAccountList(activeAccounts = 2)
            coEvery { multiAccountListSupplier.getSyncOrNull(Unit, any()) } returns listOf(accountList1, accountList2)

            // Act
            val actual = useCase.invokeSync()

            // Assert
            Truth.assertThat(actual).isTrue()
        }
    }

    private fun createAccountList(activeAccounts: Int): AccountList = mockk {
        every { this@mockk.activeAccounts } returns activeAccounts
    }
}