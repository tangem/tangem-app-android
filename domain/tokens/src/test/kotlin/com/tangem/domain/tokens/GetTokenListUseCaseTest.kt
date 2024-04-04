package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.mock.MockNetworks
import com.tangem.domain.tokens.mock.MockQuotes
import com.tangem.domain.tokens.mock.MockTokenLists
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.repository.MockCurrenciesRepository
import com.tangem.domain.tokens.repository.MockNetworksRepository
import com.tangem.domain.tokens.repository.MockQuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test

internal class GetTokenListUseCaseTest {

    private val userWalletId = UserWalletId(value = null)

    @Ignore
    @Test
    fun `when list ungrouped and unsorted then correct token list should be returned`() = runTest {
        // Given
        val expectedResult = listOf(
            MockTokenLists.loadingUngroupedTokenList.right(),
            MockTokenLists.failedUngroupedTokenList.right(),
        )

        val useCase = getUseCase(
            isGrouped = flowOf(false.right()),
            isSortedByBalance = flowOf(false.right()),
        )

        // When
        val result = useCase(userWalletId)
            .take(count = 2)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when tokens getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(tokens = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes getting failed then token list without quotes should be received`() = runTest {
        // Given
        val expectedResult = listOf(
            MockTokenLists.loadingUngroupedTokenList.right(),
            MockTokenLists.noQuotesUngroupedTokenList.right(),
        )

        val useCase = getUseCase(
            quotes = flowOf(DataError.NetworkError.NoInternetConnection.left()),
            statuses = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
        )

        // When
        val result = useCase(userWalletId)
            .take(count = 2)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when grouping type getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(isGrouped = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when sorting type getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(isSortedByBalance = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Ignore
    @Test
    fun `when tokens getting failed on second emit then error should be received`() = runTest {
        // Given
        val error = DataError.NetworkError.NoInternetConnection.left()
        val expectedResult = listOf(
            MockTokenLists.loadingUngroupedTokenList.right(),
            MockTokenLists.failedUngroupedTokenList.right(),
            TokenListError.DataError(DataError.NetworkError.NoInternetConnection).left(),
        )

        val useCase = getUseCase(
            tokens = flowOf(
                MockTokens.tokens.right(),
                error,
            ).map { delay(timeMillis = 1_000); it },
        )

        // When
        val result = useCase(userWalletId)
            .take(count = 3)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Ignore
    @Test
    fun `when list grouped then correct token list should be received`() = runTest {
        val expectedResult = listOf(
            MockTokenLists.loadingGroupedTokenList.right(),
            MockTokenLists.failedGroupedTokenList.right(),
        )

        val useCase = getUseCase(isGrouped = flowOf(true.right()))

        // When
        val result = useCase(userWalletId)
            .take(count = 2)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is sorted and ungrouped then correct token list should be received`() = runTest {
        val expectedResult = listOf(
            MockTokenLists.loadingUngroupedTokenList.copy(sortedBy = TokenList.SortType.BALANCE).right(),
            MockTokenLists.sortedUngroupedTokenList.right(),
        )

        val useCase = getUseCase(
            statuses = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
            isGrouped = flowOf(false.right()),
            isSortedByBalance = flowOf(true.right()),
        )

        // When
        val result = useCase(userWalletId)
            .take(count = 2)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is sorted and grouped then correct token list should be received`() = runTest {
        val expectedResult = listOf(
            MockTokenLists.loadingGroupedTokenList.copy(sortedBy = TokenList.SortType.BALANCE).right(),
            MockTokenLists.sortedGroupedTokenList.right(),
        )

        val useCase = getUseCase(
            statuses = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
            isGrouped = flowOf(true.right()),
            isSortedByBalance = flowOf(true.right()),
        )

        // When
        val result = useCase(userWalletId)
            .take(count = 2)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when tokens is empty then not initialized token list should be received`() = runTest {
        val expectedResult = MockTokenLists.emptyTokenList.right()

        val useCase = getUseCase(tokens = flowOf(emptyList<CryptoCurrency>().right()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when tokens flow is empty then error should be received`() = runTest {
        val expectedResult = TokenListError.EmptyTokens.left()

        val useCase = getUseCase(tokens = flowOf())

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks statuses flow is empty then error should be received`() = runTest {
        val expectedResult = listOf(
            MockTokenLists.loadingUngroupedTokenList.right(),
            TokenListError.EmptyTokens.left(),
        )

        val useCase = getUseCase(statuses = flowOf())

        // When
        val result = useCase(userWalletId)
            .take(count = 2)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks statuses is empty then loading token list should be received`() = runTest {
        val expectedResult = MockTokenLists.loadingUngroupedTokenList.right()

        val useCase = getUseCase(statuses = flowOf(emptySet<NetworkStatus>().right()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes flow is empty then list without quotes should be received`() = runTest {
        val expectedResult = listOf(
            MockTokenLists.loadingUngroupedTokenList.right(),
            MockTokenLists.noQuotesUngroupedTokenList.right(),
        )

        val useCase = getUseCase(
            statuses = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
            quotes = flowOf(emptySet<Quote>().right()),
        )

        // When
        val result = useCase(userWalletId)
            .take(count = 2)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes is empty and statuses verified then loading token list should be received`() = runTest {
        val expectedResult = MockTokenLists.loadingUngroupedTokenList.right()

        val useCase = getUseCase(
            statuses = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
            quotes = flowOf(emptySet<Quote>().right()),
        )

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    private fun getUseCase(
        tokens: Flow<Either<DataError, List<CryptoCurrency>>> = flowOf(MockTokens.tokens.right()),
        quotes: Flow<Either<DataError, Set<Quote>>> = flowOf(MockQuotes.quotes.right()),
        statuses: Flow<Either<DataError, Set<NetworkStatus>>> = flowOf(MockNetworks.errorNetworksStatuses.right()),
        isGrouped: Flow<Either<DataError, Boolean>> = flowOf(MockTokenLists.isGrouped.right()),
        isSortedByBalance: Flow<Either<DataError, Boolean>> = flowOf(MockTokenLists.isSortedByBalance.right()),
    ) = GetTokenListUseCase(
        currenciesRepository = MockCurrenciesRepository(
            sortTokensResult = Unit.right(),
            removeCurrencyResult = Unit.right(),
            token = MockTokens.token1.right(),
            tokens = tokens,
            isGrouped = isGrouped,
            isSortedByBalance = isSortedByBalance,
        ),
        quotesRepository = MockQuotesRepository(quotes),
        networksRepository = MockNetworksRepository(statuses),
    )
}
