package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.mock.MockNetworks
import com.tangem.domain.tokens.mock.MockQuotes
import com.tangem.domain.tokens.mock.MockTokens
import com.tangem.domain.tokens.mock.MockTokensStates
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.operations.CachedCurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.MockCurrenciesRepository
import com.tangem.domain.tokens.repository.MockNetworksRepository
import com.tangem.domain.tokens.repository.MockQuotesRepository
import com.tangem.domain.tokens.repository.MockStakingRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

internal class GetPrimaryCurrencyStatusUpdatesUseCaseTest {

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
        val expectedResult = CurrencyStatusError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(token = DataError.NetworkError.NoInternetConnection.left())

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes getting failed then currency with no quote status should be received`() = runTest {
        // Given
        val expectedResult = MockTokensStates.noQuotesTokensStatuses.first().right()

        val useCase = getUseCase(quotes = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks statuses getting failed then error should be received`() = runTest {
        // Given
        val expectedResult = CurrencyStatusError.DataError(DataError.NetworkError.NoInternetConnection).left()

        val useCase = getUseCase(statuses = flowOf(DataError.NetworkError.NoInternetConnection.left()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when networks statuses flow is empty then error should be received`() = runTest {
        val expectedResult = CurrencyStatusError.UnableToCreateCurrency.left()

        val useCase = getUseCase(statuses = flowOf())

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes flow is empty then no quote status should be received`() = runTest {
        val expectedResult = MockTokensStates.noQuotesTokensStatuses.first().right()

        val useCase = getUseCase(quotes = flowOf(emptySet<Quote>().right()))

        // When
        val result = useCase(userWalletId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when quotes are empty and statuses are verified then token without quote should be received`() = runTest {
        val expectedResult = with(MockTokensStates.tokenState1) {
            copy(
                value = CryptoCurrencyStatus.NoQuote(
                    amount = BigDecimal.TEN,
                    hasCurrentNetworkTransactions = false,
                    pendingTransactions = emptySet(),
                    networkAddress = NetworkAddress.Single(
                        defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
                    ),
                    yieldBalance = null,
                    sources = CryptoCurrencyStatus.Sources(),
                ),
            )
        }
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
    fun `when quotes are loaded and statuses are empty then error be received`() = runTest {
        val expectedResult = CurrencyStatusError.UnableToCreateCurrency.left()

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
        removeCurrencyResult: Either<DataError, Unit> = Unit.right(),
        quotes: Flow<Either<DataError, Set<Quote>>> = flowOf(MockQuotes.quotes.right()),
        statuses: Flow<Either<DataError, Set<NetworkStatus>>> = flowOf(MockNetworks.verifiedNetworksStatuses.right()),
    ) = GetPrimaryCurrencyStatusUpdatesUseCase(
        currencyStatusOperations = CachedCurrenciesStatusesOperations(
            currenciesRepository = MockCurrenciesRepository(
                sortTokensResult = Unit.right(),
                removeCurrencyResult = removeCurrencyResult,
                token = token,
                tokens = flowOf(),
                isGrouped = flowOf(),
                isSortedByBalance = flowOf(),
            ),
            quotesRepository = MockQuotesRepository(quotes),
            networksRepository = MockNetworksRepository(statuses),
            stakingRepository = MockStakingRepository(),

            tokensFeatureToggles = object : TokensFeatureToggles {
                override val isNetworksLoadingRefactoringEnabled: Boolean = false
            },
            singleNetworkStatusSupplier = mockk(),
            multiNetworkStatusFetcher = mockk(),
            multiNetworkStatusSupplier = mockk(),
        ),
        dispatchers = dispatchers,
    )
}