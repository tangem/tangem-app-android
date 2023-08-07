package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.error.TokenError
import com.tangem.domain.tokens.mock.MockNetworks
import com.tangem.domain.tokens.mock.MockQuotes
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.domain.tokens.mock.MockTokensStates
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.MockNetworksRepository
import com.tangem.domain.tokens.repository.MockQuotesRepository
import com.tangem.domain.tokens.repository.MockTokensRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GetPrimaryCurrencyUseCaseTest {

    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val userWalletId = UserWalletId(value = null)

    @Test
    fun `when all data received then token should be received`() = runTest {
        // Given
        val expectedResult = MockTokensStates.loadedTokensStates.first().right()

        val useCase = getUseCase()

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when token getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(token = DataError.NetworkError.NoInternetConnection.left())

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(quotes = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks statuses getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = TokenError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(statuses = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks statuses flow is empty then error should be received`() = runTest {
        val expectedResult = TokenError.UnableToCreateToken.left()

        val useCase = getUseCase(statuses = flowOf())

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes flow is empty then error should be received`() = runTest {
        val expectedResult = TokenError.UnableToCreateToken.left()

        val useCase = getUseCase(quotes = flowOf())

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes are empty and statuses are verified then loading token should be received`() = runTest {
        val expectedResult = MockTokensStates.tokenState1
            .copy(value = CryptoCurrencyStatus.Loading)
            .right()

        val useCase = getUseCase(
            statuses = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
            quotes = flowOf(emptySet<Quote>().right()),
        )

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes are loaded and statuses are empty then loading token should be received`() = runTest {
        val expectedResult = MockTokensStates.tokenState1
            .copy(value = CryptoCurrencyStatus.Loading)
            .right()

        val useCase = getUseCase(
            statuses = flowOf(emptySet<NetworkStatus>().right()),
            quotes = flowOf(MockQuotes.quotes.right()),
        )

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    private fun getUseCase(
        token: Either<DataError, CryptoCurrency> = MockTokens.token1.right(),
        quotes: Flow<Either<DataError, Set<Quote>>> = flowOf(MockQuotes.quotes.right()),
        statuses: Flow<Either<DataError, Set<NetworkStatus>>> = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
    ) = GetPrimaryCurrencyUseCase(
        dispatchers = dispatchers,
        tokensRepository = MockTokensRepository(
            sortTokensResult = Unit.right(),
            token = token,
            tokens = flowOf(),
            isGrouped = flowOf(),
            isSortedByBalance = flowOf(),
        ),
        quotesRepository = MockQuotesRepository(quotes),
        networksRepository = MockNetworksRepository(MockNetworks.networks.right(), statuses),
    )
}
