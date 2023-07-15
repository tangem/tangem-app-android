package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.domain.tokens.repository.MockTokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class ApplyTokenListSortingUseCaseTest {

    private val userWalletId = UserWalletId(value = null)

    @Test
    fun `when tokens are empty then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.TokenListIsEmpty.left()

        val useCase = getUseCase()

        // When
        val result = useCase(
            userWalletId = userWalletId,
            sortedTokens = emptySet(),
            isGrouped = false,
            isSortedByBalance = false,
        )

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when tokens saving failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val repository = getTokensRepository(
            sortTokensResult = DataError.NetworkError.NoInternetConnection.left(),
        )
        val useCase = getUseCase(repository)

        // When
        val result = useCase(
            userWalletId = userWalletId,
            sortedTokens = MockTokens.tokens,
            isGrouped = false,
            isSortedByBalance = false,
        )

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when apply sorting for sorted and grouped list then correct args should be used`() = runTest {
        // Given
        val expectedTokens = MockTokens.tokens
        val expectedIsGrouped = true
        val expectedIsSorted = true

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        useCase(
            userWalletId = userWalletId,
            sortedTokens = expectedTokens,
            isGrouped = expectedIsGrouped,
            isSortedByBalance = expectedIsSorted,
        )

        // Then
        assertEquals(expectedTokens, repository.tokensIdsAfterSortingApply)
        assertEquals(expectedIsGrouped, repository.isTokensGroupedAfterSortingApply)
        assertEquals(expectedIsSorted, repository.isTokensSortedByBalanceAfterSortingApply)
    }

    @Test
    fun `when apply sorting for unsorted and grouped list then correct args should be used`() = runTest {
        // Given
        val expectedTokens = MockTokens.tokens
        val expectedIsGrouped = true
        val expectedIsSorted = false

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        useCase(
            userWalletId = userWalletId,
            sortedTokens = expectedTokens,
            isGrouped = expectedIsGrouped,
            isSortedByBalance = expectedIsSorted,
        )

        // Then
        assertEquals(expectedTokens, repository.tokensIdsAfterSortingApply)
        assertEquals(expectedIsGrouped, repository.isTokensGroupedAfterSortingApply)
        assertEquals(expectedIsSorted, repository.isTokensSortedByBalanceAfterSortingApply)
    }

    @Test
    fun `when apply sorting for sorted and ungrouped list then correct args should be used`() = runTest {
        // Given
        val expectedTokens = MockTokens.tokens
        val expectedIsGrouped = false
        val expectedIsSorted = true

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        useCase(
            userWalletId = userWalletId,
            sortedTokens = expectedTokens,
            isGrouped = expectedIsGrouped,
            isSortedByBalance = expectedIsSorted,
        )

        // Then
        assertEquals(expectedTokens, repository.tokensIdsAfterSortingApply)
        assertEquals(expectedIsGrouped, repository.isTokensGroupedAfterSortingApply)
        assertEquals(expectedIsSorted, repository.isTokensSortedByBalanceAfterSortingApply)
    }

    @Test
    fun `when apply sorting for unsorted and ungrouped list then correct args should be used`() = runTest {
        // Given
        val expectedTokens = MockTokens.tokens
        val expectedIsGrouped = false
        val expectedIsSorted = false

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        useCase(
            userWalletId = userWalletId,
            sortedTokens = expectedTokens,
            isGrouped = expectedIsGrouped,
            isSortedByBalance = expectedIsSorted,
        )

        // Then
        assertEquals(expectedTokens, repository.tokensIdsAfterSortingApply)
        assertEquals(expectedIsGrouped, repository.isTokensGroupedAfterSortingApply)
        assertEquals(expectedIsSorted, repository.isTokensSortedByBalanceAfterSortingApply)
    }

    private fun getUseCase(tokensRepository: MockTokensRepository = getTokensRepository()) =
        ApplyTokenListSortingUseCase(
            tokensRepository = tokensRepository,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

    private fun getTokensRepository(sortTokensResult: Either<DataError, Unit> = Unit.right()) =
        MockTokensRepository(sortTokensResult, emptyFlow(), emptyFlow(), emptyFlow())
}
