package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.MockTokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.random.Random

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
            sortedTokensIds = emptySet(),
            isGroupedByNetwork = false,
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
            sortedTokensIds = MockTokens.tokens.map { it.networkId to it.id }.toSet(),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when apply sorting for sorted and grouped list then correct args should be used`() = runTest {
        // Given
        val expectedTokens = getSortedTokens()
        val expectedIsGrouped = true
        val expectedIsSorted = true

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        useCase(
            userWalletId = userWalletId,
            sortedTokensIds = expectedTokens.map { it.networkId to it.id }.toSet(),
            isGroupedByNetwork = expectedIsGrouped,
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
        val expectedTokens = getSortedTokens()
        val expectedIsGrouped = true
        val expectedIsSorted = false

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        useCase(
            userWalletId = userWalletId,
            sortedTokensIds = expectedTokens.map { it.networkId to it.id }.toSet(),
            isGroupedByNetwork = expectedIsGrouped,
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
        val expectedTokens = getSortedTokens()
        val expectedIsGrouped = false
        val expectedIsSorted = true

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        useCase(
            userWalletId = userWalletId,
            sortedTokensIds = expectedTokens.map { it.networkId to it.id }.toSet(),
            isGroupedByNetwork = expectedIsGrouped,
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
        val expectedTokens = getSortedTokens()
        val expectedIsGrouped = false
        val expectedIsSorted = false

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        useCase(
            userWalletId = userWalletId,
            sortedTokensIds = expectedTokens.map { it.networkId to it.id }.toSet(),
            isGroupedByNetwork = expectedIsGrouped,
            isSortedByBalance = expectedIsSorted,
        )

        // Then
        assertEquals(expectedTokens, repository.tokensIdsAfterSortingApply)
        assertEquals(expectedIsGrouped, repository.isTokensGroupedAfterSortingApply)
        assertEquals(expectedIsSorted, repository.isTokensSortedByBalanceAfterSortingApply)
    }

    @Test
    fun `when sorted tokens IDs do not contain all tokens IDs then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListSortingError.UnableToSortTokenList.left()

        val repository = getTokensRepository()
        val useCase = getUseCase(repository)

        // When
        val result = useCase(
            userWalletId = userWalletId,
            sortedTokensIds = getSortedTokens().drop(n = 3).map { it.networkId to it.id }.toSet(),
            isGroupedByNetwork = false,
            isSortedByBalance = false,
        )

        // Then
        assertEquals(expectedResult, result)
    }

    private fun getSortedTokens() = MockTokens.tokens
        .sortedBy { Random.nextInt(0, MockTokens.tokens.size) }
        .toSet()

    private fun getUseCase(tokensRepository: MockTokensRepository = getTokensRepository()) =
        ApplyTokenListSortingUseCase(
            tokensRepository = tokensRepository,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

    private fun getTokensRepository(
        sortTokensResult: Either<DataError, Unit> = Unit.right(),
        tokens: Flow<Either<DataError, Set<Token>>> = flowOf(MockTokens.tokens.right()),
    ): MockTokensRepository {
        return MockTokensRepository(sortTokensResult, tokens, emptyFlow(), emptyFlow())
    }
}