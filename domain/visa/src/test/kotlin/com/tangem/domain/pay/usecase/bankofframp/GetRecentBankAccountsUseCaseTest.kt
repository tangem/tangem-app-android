package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.repository.BankOfframpRecentsStore
import com.tangem.domain.pay.repository.BankOfframpRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetRecentBankAccountsUseCaseTest {

    private val repository: BankOfframpRepository = mockk()
    private val recentsStore: BankOfframpRecentsStore = mockk()
    private val useCase = GetRecentBankAccountsUseCase(repository = repository, recentsStore = recentsStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, recentsStore)
    }

    @Test
    fun `GIVEN usage for four accounts WHEN invoke THEN three most recent in order`() = runTest {
        // Arrange
        val accounts = listOf("a", "b", "c", "d", "e").map { bankAccount(id = it, displayName = it) }
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns accounts.right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns mapOf("a" to 10L, "b" to 40L, "c" to 30L, "d" to 20L)

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.map { it.id }).containsExactly("b", "c", "d").inOrder()
    }

    @Test
    fun `GIVEN usage of a deleted account WHEN invoke THEN it is skipped`() = runTest {
        // Arrange
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns listOf(bankAccount(id = "a")).right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns mapOf("a" to 1L, "gone" to 2L)

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()?.map { it.id }).containsExactly("a")
    }

    @Test
    fun `GIVEN no usage WHEN invoke THEN empty list`() = runTest {
        // Arrange
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns listOf(bankAccount()).right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns emptyMap()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        assertThat(actual.getOrNull()).isEmpty()
    }
}