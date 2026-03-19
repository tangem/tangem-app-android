package com.tangem.domain.tokens.wallet

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.tokens.FetchingSource
import com.tangem.domain.tokens.wallet.implementor.MultiWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.implementor.SingleWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.implementor.SingleWalletWithTokenBalanceFetcher
import com.tangem.test.core.assertEither
import com.tangem.test.core.assertEitherRight
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletBalanceFetcherTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val expressServiceFetcher: ExpressServiceFetcher = mockk()
    private val multiWalletBalanceFetcher: MultiWalletBalanceFetcher = mockk()
    private val singleWalletWithTokenBalanceFetcher: SingleWalletWithTokenBalanceFetcher = mockk()
    private val singleWalletBalanceFetcher: SingleWalletBalanceFetcher = mockk()
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher = mockk()
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher = mockk()
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()

    private val fetcher = WalletBalanceFetcher(
        userWalletsListRepository = userWalletsListRepository,
        expressServiceFetcher = expressServiceFetcher,
        multiWalletBalanceFetcher = multiWalletBalanceFetcher,
        singleWalletWithTokenBalanceFetcher = singleWalletWithTokenBalanceFetcher,
        singleWalletBalanceFetcher = singleWalletBalanceFetcher,
        multiNetworkStatusFetcher = multiNetworkStatusFetcher,
        multiQuoteStatusFetcher = multiQuoteStatusFetcher,
        multiStakingBalanceFetcher = multiStakingBalanceFetcher,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        stakingIdFactory = stakingIdFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            userWalletsListRepository,
            expressServiceFetcher,
            multiWalletBalanceFetcher,
            singleWalletWithTokenBalanceFetcher,
            singleWalletBalanceFetcher,
            multiNetworkStatusFetcher,
            multiQuoteStatusFetcher,
            multiStakingBalanceFetcher,
        )
        mockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    @AfterEach
    fun tearDownStaticMocks() {
        unmockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    @Test
    fun `fetch failure if getSyncStrict THROWS EXCEPTION`() = runTest {
        // Arrange
        val exception = IllegalStateException("Error")
        every { userWalletsListRepository.getSyncStrict(userWalletId) } throws exception

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = exception.left()
        assertEither(actual, expected)

        verifyOrder { userWalletsListRepository.getSyncStrict(userWalletId) }

        coVerify(inverse = true) {
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if cardTypesResolver cannot resolve wallet type`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns false
            every { isSingleWalletWithToken() } returns false
            every { isSingleWallet() } returns false
        }

        mockColdWallet(cardTypesResolver)

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = IllegalStateException("Unknown type of wallet: $userWalletId").left()
        assertEither(actual, expected)

        verifyOrder { userWalletsListRepository.getSyncStrict(userWalletId) }

        coVerify(inverse = true) {
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if getCryptoCurrencies THROWS EXCEPTION`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val exception = IllegalStateException("Error")

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } throws exception

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = exception.left()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if getCryptoCurrencies RETURNS EMPTY SET`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns emptySet()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = IllegalStateException("UserWallet doesn't contain crypto-currencies: $userWalletId").left()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if fetchNetworks RETURNS LEFT`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val exception = IllegalStateException("Error")

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.NETWORK)),
        )
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns exception.left()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = IllegalStateException(
            "Failed to fetch next sources for UserWalletId(011...011):\n" +
                "NETWORK – java.lang.IllegalStateException: Error",
        ).left()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiWalletBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            multiQuoteStatusFetcher(params = any())
            stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if fetchQuotes RETURNS LEFT`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        val exception = IllegalStateException("Error")

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.QUOTE)),
        )
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns exception.left()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = IllegalStateException(
            "Failed to fetch next sources for UserWalletId(011...011):\n" +
                "QUOTE – java.lang.IllegalStateException: Error",
        ).left()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiWalletBalanceFetcher.fetchingSources
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            multiNetworkStatusFetcher(params = any())
            stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if fetchStaking RETURNS LEFT`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val stakingBalanceFetcherParams = MultiStakingBalanceFetcher.Params(
            userWalletId = userWalletId,
            stakingIds = setOf(ethereumStakingId, stellarStakingId),
        )

        val exception = IllegalStateException("Error")

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.STAKING)),
        )
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
        } returns Either.Right(ethereumStakingId)
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
        } returns Either.Right(stellarStakingId)
        coEvery { multiStakingBalanceFetcher(params = stakingBalanceFetcherParams) } returns exception.left()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = IllegalStateException(
            "Failed to fetch next sources for UserWalletId(011...011):\n" +
                "STAKING – java.lang.IllegalStateException: Error",
        ).left()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiWalletBalanceFetcher.fetchingSources
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
            multiStakingBalanceFetcher(params = stakingBalanceFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if stakingIdFactory RETURNS UnsupportedCurrency for all currencies`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.STAKING)),
        )
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = any())
        } returns Either.Left(StakingIdFactory.Error.UnsupportedCurrency)

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        assertEitherRight(actual)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiWalletBalanceFetcher.fetchingSources
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if stakingIdFactory RETURNS UnableToGetAddress for all currencies`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val stakingId = Either.Left(
            StakingIdFactory.Error.UnableToGetAddress(
                integrationId = StakingIntegrationID.StakeKit.EthereumToken.Polygon,
            ),
        )

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.STAKING)),
        )
        coEvery { stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = any()) } returns stakingId

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        assertEitherRight(actual)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiWalletBalanceFetcher.fetchingSources
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if stakingIdFactory RETURNS UnableToGetAddress and UnsupportedCurrency`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val ethereumStakingId = Either.Left(
            StakingIdFactory.Error.UnableToGetAddress(
                integrationId = StakingIntegrationID.StakeKit.EthereumToken.Polygon,
            ),
        )
        val stellarStakingId = Either.Left(StakingIdFactory.Error.UnsupportedCurrency)

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.STAKING)),
        )
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
        } returns ethereumStakingId
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
        } returns stellarStakingId

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        assertEitherRight(actual)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiWalletBalanceFetcher.fetchingSources
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            multiNetworkStatusFetcher(params = any())
            multiQuoteStatusFetcher(params = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch failure if all fetching sources RETURNS LEFT`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        val stakingBalanceFetcherParams = MultiStakingBalanceFetcher.Params(
            userWalletId = userWalletId,
            stakingIds = setOf(ethereumStakingId, stellarStakingId),
        )

        val exception = IllegalStateException("Error")

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.STAKING)),
        )
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns exception.left()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns exception.left()
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
        } returns Either.Right(ethereumStakingId)
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
        } returns Either.Right(stellarStakingId)
        coEvery { multiStakingBalanceFetcher(params = stakingBalanceFetcherParams) } returns exception.left()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = IllegalStateException(
            "Failed to fetch next sources for UserWalletId(011...011):\n" +
                "NETWORK – java.lang.IllegalStateException: Error\n" +
                "QUOTE – java.lang.IllegalStateException: Error\n" +
                "STAKING – java.lang.IllegalStateException: Error",
        ).left()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiWalletBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
            multiStakingBalanceFetcher(params = stakingBalanceFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
        }
    }

    @Test
    fun `fetch successfully for multi-currency wallet`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        val stakingBalanceFetcherParams = MultiStakingBalanceFetcher.Params(
            userWalletId = userWalletId,
            stakingIds = setOf(ethereumStakingId, stellarStakingId),
        )

        mockColdWallet(cardTypesResolver)
        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.STAKING)),
        )
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns Unit.right()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns Unit.right()
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
        } returns Either.Right(ethereumStakingId)
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
        } returns Either.Right(stellarStakingId)
        coEvery { multiStakingBalanceFetcher(params = stakingBalanceFetcherParams) } returns Unit.right()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            multiWalletBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
            multiStakingBalanceFetcher(params = stakingBalanceFetcherParams)
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
        }
    }

    @Test
    fun `fetch successfully for single-currency with token wallet`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns false
            every { isSingleWalletWithToken() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        mockColdWallet(cardTypesResolver)
        coEvery {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
        } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { singleWalletWithTokenBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.NETWORK, FetchingSource.QUOTE)),
        )
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns Unit.right()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns Unit.right()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            singleWalletWithTokenBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
        }

        coVerify(inverse = true) {
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch successfully for single-currency wallet`() = runTest {
        // Arrange
        val cardTypesResolver = mockk<CardTypesResolver> {
            every { isMultiwalletAllowed() } returns false
            every { isSingleWalletWithToken() } returns false
            every { isSingleWallet() } returns true
        }

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        mockColdWallet(cardTypesResolver)
        coEvery { singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { singleWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.NETWORK, FetchingSource.QUOTE)),
        )
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns Unit.right()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns Unit.right()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
            singleWalletBalanceFetcher.fetchingSources
            multiNetworkStatusFetcher(params = networkStatusFetcherParams)
            multiQuoteStatusFetcher(params = quoteStatusFetcherParams)
        }

        coVerify(inverse = true) {
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            multiStakingBalanceFetcher(params = any())
        }
    }

    @Test
    fun `fetch successfully for hot wallet`() = runTest {
        // Arrange
        val hotWallet = mockk<UserWallet.Hot>()
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns hotWallet

        val currencies = cryptoCurrencyFactory.ethereumAndStellar.toSet()

        val networkStatusFetcherParams = MultiNetworkStatusFetcher.Params(
            userWalletId = userWalletId,
            networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network),
        )

        val quoteStatusFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = currencies.mapNotNullTo(destination = hashSetOf(), transform = { it.id.rawCurrencyId }),
            appCurrencyId = null,
        )

        val stakingBalanceFetcherParams = MultiStakingBalanceFetcher.Params(
            userWalletId = userWalletId,
            stakingIds = setOf(ethereumStakingId, stellarStakingId),
        )

        coEvery { multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any()) } returns currencies
        coEvery { expressServiceFetcher.fetch(userWallet = any(), assetIds = any()) } returns mockk()
        every { multiWalletBalanceFetcher.fetchingSources } returns setOf(
            WalletFetchingSource.Balance(setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.STAKING)),
        )
        coEvery { multiNetworkStatusFetcher(params = networkStatusFetcherParams) } returns Unit.right()
        coEvery { multiQuoteStatusFetcher(params = quoteStatusFetcherParams) } returns Unit.right()
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.ethereum)
        } returns Either.Right(ethereumStakingId)
        coEvery {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = cryptoCurrencyFactory.stellar)
        } returns Either.Right(stellarStakingId)
        coEvery { multiStakingBalanceFetcher(params = stakingBalanceFetcherParams) } returns Unit.right()

        // Act
        val actual = fetcher(
            params = WalletBalanceFetcher.Params(
                userWalletId = userWalletId,
                isPaymentAccountRefactorEnabled = false,
            ),
        )

        // Assert
        val expected = Unit.right()
        assertEither(actual, expected)

        coVerifyOrder {
            userWalletsListRepository.getSyncStrict(userWalletId)
            multiWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
            expressServiceFetcher.fetch(userWallet = any(), assetIds = any())
        }

        coVerify(inverse = true) {
            singleWalletWithTokenBalanceFetcher.getCryptoCurrencies(userWallet = any())
            singleWalletBalanceFetcher.getCryptoCurrencies(userWallet = any())
        }
    }

    private fun mockColdWallet(cardTypesResolver: CardTypesResolver) {
        val coldWallet = mockk<UserWallet.Cold>()
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet
        mockkStatic(UserWallet.Cold::cardTypesResolver)
        every { coldWallet.cardTypesResolver } returns cardTypesResolver
    }

    private companion object {

        val userWalletId = UserWalletId("011")
        val ethereumStakingId = StakingID(integrationId = "ethereum", address = "0x1")
        val stellarStakingId = StakingID(integrationId = "stellar", address = "0x1")
    }
}