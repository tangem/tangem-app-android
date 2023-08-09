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
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.MockCurrenciesRepository
import com.tangem.domain.tokens.repository.MockNetworksRepository
import com.tangem.domain.tokens.repository.MockQuotesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetTokenListUseCaseTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val userWalletId = UserWalletId(value = null)

    @Test
    fun `when list ungrouped and unsorted then correct token list should be returned`() = runTest {
        // Given
        val expectedResult = MockTokenLists.failedUngroupedTokenList.right()

        val useCase = getUseCase(
            isGrouped = flowOf(false.right()),
            isSortedByBalance = flowOf(false.right()),
        )

        // When
        val result = useCase(userWalletId).first()

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
    fun `when quotes getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(quotes = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks getting failed and list is groped then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(
            networks = DataError.NetworkError.NoInternetConnection.left(),
            isGrouped = flowOf(true.right()),
        )

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks statuses getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenListError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(statuses = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

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

    @Test
    fun `when tokens getting failed on second emit then error should be received`() = runTest {
        // Given
        val error = DataError.NetworkError.NoInternetConnection.left()
        val expectedResult = listOf(
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
            .take(count = 2)
            .toList()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list grouped then correct token list should be received`() = runTest {
        val expectedResult = MockTokenLists.failedGroupedTokenList.right()

        val useCase = getUseCase(isGrouped = flowOf(true.right()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is grouped and networks getting failed then error should be received`() = runTest {
        val expectedResult = TokenListError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(
            networks = DataError.NetworkError.NoInternetConnection.left(),
            isGrouped = flowOf(true.right()),
        )

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is sorted and ungrouped then correct token list should be received`() = runTest {
        val expectedResult = MockTokenLists.sortedUngroupedTokenList.right()

        val useCase = getUseCase(
            statuses = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
            isGrouped = flowOf(false.right()),
            isSortedByBalance = flowOf(true.right()),
        )

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is sorted and grouped then correct token list should be received`() = runTest {
        val expectedResult = MockTokenLists.sortedGroupedTokenList.right()

        val useCase = getUseCase(
            statuses = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
            isGrouped = flowOf(true.right()),
            isSortedByBalance = flowOf(true.right()),
        )

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when tokens is empty then not initialized token list should be received`() = runTest {
        val expectedResult = MockTokenLists.notInitializedTokenList.right()

        val useCase = getUseCase(tokens = flowOf(emptySet<CryptoCurrency>().right()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks is empty and list is grouped then ungrouped list should be received`() = runTest {
        val expectedResult = TokenListError.UnableToSortTokenList(MockTokenLists.failedUngroupedTokenList).left()

        val useCase = getUseCase(
            networks = emptySet<Network>().right(),
            isGrouped = flowOf(true.right()),
        )

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
        val expectedResult = TokenListError.EmptyTokens.left()

        val useCase = getUseCase(statuses = flowOf())

        // When
        val result = useCase(userWalletId).first()

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
    fun `when quotes flow is empty then error should be received`() = runTest {
        val expectedResult = TokenListError.EmptyTokens.left()

        val useCase = getUseCase(quotes = flowOf())

        // When
        val result = useCase(userWalletId).first()

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
        tokens: Flow<Either<DataError, Set<CryptoCurrency>>> = flowOf(MockTokens.tokens.right()),
        quotes: Flow<Either<DataError, Set<Quote>>> = flowOf(MockQuotes.quotes.right()),
        networks: Either<DataError, Set<Network>> = MockNetworks.networks.right(),
        statuses: Flow<Either<DataError, Set<NetworkStatus>>> = flowOf(MockNetworks.errorNetworksStatuses.right()),
        isGrouped: Flow<Either<DataError, Boolean>> = flowOf(MockTokenLists.isGrouped.right()),
        isSortedByBalance: Flow<Either<DataError, Boolean>> = flowOf(MockTokenLists.isSortedByBalance.right()),
    ) = GetTokenListUseCase(
        dispatchers = dispatchers,
        currenciesRepository = MockCurrenciesRepository(
            sortTokensResult = Unit.right(),
            token = MockTokens.token1.right(),
            tokens = tokens,
            isGrouped = isGrouped,
            isSortedByBalance = isSortedByBalance,
        ),
        quotesRepository = MockQuotesRepository(quotes),
        networksRepository = MockNetworksRepository(networks, statuses),
    )
}
