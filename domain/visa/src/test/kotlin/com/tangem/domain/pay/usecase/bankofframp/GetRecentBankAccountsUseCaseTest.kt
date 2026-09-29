package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.RecentBankAccounts
import com.tangem.domain.pay.repository.BankOfframpRecentsStore
import com.tangem.domain.pay.repository.BankOfframpRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
            hasMoreAccounts = true,
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
        val expected = RecentBankAccounts(recents = listOf(bankAccount(id = "a")), hasMoreAccounts = false)
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN saved accounts without usage WHEN invoke THEN first three in see all order returned`() = runTest {
        // Arrange
        val accounts = listOf("Dave", "charlie", "Bob", "alice").map { bankAccount(id = it, displayName = it) }
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns accounts.right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns emptyMap()

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        val expected = RecentBankAccounts(
            recents = listOf("alice", "Bob", "charlie").map { bankAccount(id = it, displayName = it) },
            hasMoreAccounts = true,
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN some accounts used WHEN invoke THEN used ones first and the rest in see all order`() = runTest {
        // Arrange
        val accounts = listOf("c", "b", "a", "d").map { bankAccount(id = it, displayName = it) }
        coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns accounts.right()
        coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns mapOf("d" to 5L)

        // Act
        val actual = useCase(TEST_USER_WALLET_ID)

        // Assert
        val expected = RecentBankAccounts(
            recents = listOf("d", "a", "b").map { bankAccount(id = it, displayName = it) },
            hasMoreAccounts = true,
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN saved accounts count WHEN invoke THEN more accounts flagged only above three`(model: CountModel) =
        runTest {
            // Arrange
            val accounts = List(model.count) { bankAccount(id = "acc-$it", displayName = "acc-$it") }
            coEvery { repository.getBankAccounts(TEST_USER_WALLET_ID) } returns accounts.right()
            coEvery { recentsStore.getUsage(TEST_USER_WALLET_ID) } returns emptyMap()

            // Act
            val actual = useCase(TEST_USER_WALLET_ID)

            // Assert
            val expected = RecentBankAccounts(
                recents = accounts.take(model.expectedShown),
                hasMoreAccounts = model.expectedHasMore,
            )
            assertThat(actual).isEqualTo(expected.right())
        }

    internal data class CountModel(val count: Int, val expectedShown: Int, val expectedHasMore: Boolean)

    private fun provideTestModels() = listOf(
        CountModel(count = 0, expectedShown = 0, expectedHasMore = false),
        CountModel(count = 1, expectedShown = 1, expectedHasMore = false),
        CountModel(count = 3, expectedShown = 3, expectedHasMore = false),
        CountModel(count = 4, expectedShown = 3, expectedHasMore = true),
    )
}