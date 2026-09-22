package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.RecentBankAccounts
import com.tangem.domain.pay.repository.BankOfframpRecentsStore
import com.tangem.domain.pay.repository.BankOfframpRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
    fun `GIVEN usage log WHEN invoke THEN three most recent returned newest first`() = runTest {
        // Arrange
        val accounts = listOf("a", "b", "c", "d", "e").map { bankAccount(id = it, displayName = it) }
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns accounts.right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns
            mapOf("a" to 10L, "b" to 40L, "c" to 30L, "d" to 20L)

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        val expected = RecentBankAccounts(
            recents = listOf("b", "c", "d").map { bankAccount(id = it, displayName = it) },
            hasSavedAccounts = true,
        )
        assertThat(actual).isEqualTo(expected.right())
        coVerify(exactly = 1) { repository.getBankAccounts(TEST_USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN usage of a deleted account WHEN invoke THEN it is skipped`() = runTest {
        // Arrange
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns listOf(bankAccount(id = "a")).right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns mapOf("a" to 1L, "gone" to 2L)

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        val expected = RecentBankAccounts(recents = listOf(bankAccount(id = "a")), hasSavedAccounts = true)
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN saved accounts without usage WHEN invoke THEN empty recents with has saved`() = runTest {
        // Arrange
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns listOf(bankAccount()).right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns emptyMap()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        val expected = RecentBankAccounts(recents = emptyList(), hasSavedAccounts = true)
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN no accounts at all WHEN invoke THEN empty recents without has saved`() = runTest {
        // Arrange
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns emptyList<BankAccount>().right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns mapOf("gone" to 1L)

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        val expected = RecentBankAccounts(recents = emptyList(), hasSavedAccounts = false)
        assertThat(actual).isEqualTo(expected.right())
    }
}