package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.mock.MockNetworks
import com.tangem.domain.tokens.mock.MockQuotes
import com.tangem.domain.tokens.mock.MockTokenLists
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.model.Token
import com.tangem.domain.tokens.repository.MockNetworksRepository
import com.tangem.domain.tokens.repository.MockQuotesRepository
import com.tangem.domain.tokens.repository.MockTokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class GetTokenListUseCaseTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val userWalletId = UserWalletId(value = null)

    @Test
    fun `when list ungrouped and unsorted then correct token list should be returned`() = runTest {
        // Given
        val expectedResult = MockTokenLists.ungroupedTokenList.right()

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
        val expectedResult = TokensError.EmptyTokens.left()

        val useCase = getUseCase(tokens = flowOf(expectedResult))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokensError.EmptyQuotes.left()

        val useCase = getUseCase(quotes = flowOf(expectedResult))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks getting failed and list is groped then error should be received`() = runTest {
        // Given
        val expectedResult = TokensError.EmptyNetworkStatues.left()

        val useCase = getUseCase(
            networks = expectedResult,
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
        val expectedResult = TokensError.EmptyNetworkStatues.left()

        val useCase = getUseCase(statuses = flowOf(expectedResult))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when grouping type getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokensError.EmptyTokens.left()

        val useCase = getUseCase(isGrouped = flowOf(expectedResult))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when sorting type getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokensError.EmptyTokens.left()

        val useCase = getUseCase(isSortedByBalance = flowOf(expectedResult))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when tokens getting failed on second emit then error should be received`() = runTest {
        // Given
        val error = TokensError.EmptyTokens.left()
        val expectedResult = listOf(
            MockTokenLists.ungroupedTokenList.right(),
            error,
        )

        val useCase = getUseCase(
            tokens = flowOf(
                MockTokens.tokens.right(),
                error,
            ),
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
        val expectedResult = MockTokenLists.groupedTokenList.right()

        val useCase = getUseCase(isGrouped = flowOf(true.right()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when list is grouped and networks getting failed then error should be received`() = runTest {
        val expectedResult = TokensError.EmptyNetworks.left()

        val useCase = getUseCase(
            networks = expectedResult,
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

        val useCase = getUseCase(tokens = flowOf(emptySet<Token>().right()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks is empty and list is grouped then error should be received`() = runTest {
        val expectedResult = TokensError.EmptyNetworks.left()

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
        val expectedResult = TokensError.EmptyTokens.left()

        val useCase = getUseCase(tokens = flowOf())

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks statuses flow is empty then error should be received`() = runTest {
        val expectedResult = TokensError.EmptyNetworkStatues.left()

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
        val expectedResult = TokensError.EmptyQuotes.left()

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
        tokens: Flow<Either<TokensError, Set<Token>>> = flowOf(MockTokens.tokens.right()),
        quotes: Flow<Either<TokensError, Set<Quote>>> = flowOf(MockQuotes.quotes.right()),
        networks: Either<TokensError, Set<Network>> = MockNetworks.networks.right(),
        statuses: Flow<Either<TokensError, Set<NetworkStatus>>> = flowOf(MockNetworks.errorNetworksStatuses.right()),
        isGrouped: Flow<Either<TokensError, Boolean>> = flowOf(MockTokenLists.isGrouped.right()),
        isSortedByBalance: Flow<Either<TokensError, Boolean>> = flowOf(MockTokenLists.isSortedByBalance.right()),
    ) = GetTokenListUseCase(
        dispatchers = dispatchers,
        tokensRepository = MockTokensRepository(tokens, isGrouped, isSortedByBalance),
        quotesRepository = MockQuotesRepository(quotes),
        networksRepository = MockNetworksRepository(networks, statuses),
    )
}
