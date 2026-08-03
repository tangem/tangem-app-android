package com.tangem.feature.tokendetails.domain

import arrow.core.none
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.model.warnings.DynamicAddressesWarnings
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class GetCurrencyWarningsUseCaseTest {

    private val walletManagersFacade: WalletManagersFacade = mockk(relaxed = true)
    private val currenciesRepository: CurrenciesRepository = mockk(relaxed = true)
    private val currencyChecksRepository: CurrencyChecksRepository = mockk(relaxed = true)
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier = mockk(relaxed = true)
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true)
    private val dynamicAddressesRepository: DynamicAddressesRepository = mockk(relaxed = true)
    private val dispatchers: CoroutineDispatcherProvider = TestDispatchers(Dispatchers.Unconfined)

    private val userWalletId: UserWalletId = mockk(relaxed = true)
    private val network: Network = mockk(relaxed = true)
    private val derivationPath: Network.DerivationPath = mockk(relaxed = true)
    private val accountStatusList: AccountStatusList = mockk(relaxed = true)

    private val useCase = GetCurrencyWarningsUseCase(
        walletManagersFacade = walletManagersFacade,
        currenciesRepository = currenciesRepository,
        dispatchers = dispatchers,
        currencyChecksRepository = currencyChecksRepository,
        multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        dynamicAddressesRepository = dynamicAddressesRepository,
    )

    @BeforeEach
    fun setUp() {
        // Bypass coin-related warnings flow: force both coin and token lookups to None so
        // the use case falls through to `SomeNetworksUnreachable` without needing real data.
        mockkObject(CryptoCurrencyStatusOperations)
        with(CryptoCurrencyStatusOperations) {
            every { accountStatusList.getCoinStatus(any<CryptoCurrency>()) } returns none()
            every { accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>()) } returns none()
        }

        every { singleAccountStatusListSupplier(any<UserWalletId>()) } returns flowOf(accountStatusList)
        coEvery { currencyChecksRepository.getRentInfoWarning(any(), any()) } returns null
        coEvery { currencyChecksRepository.getExistentialDeposit(any(), any()) } returns null
        coEvery { currencyChecksRepository.getFeeResourceAmount(any(), any()) } returns null
        coEvery { walletManagersFacade.getAssetRequirements(any(), any()) } returns null
        every { dynamicAddressesRepository.hasFundsOnAdditionalAddresses(any(), any()) } returns flowOf(false)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Test
    fun `GIVEN token currency WHEN invoke THEN FundsFound is absent and probe flow is not queried`() = runTest {
        // GIVEN
        val token: CryptoCurrency.Token = mockk(relaxed = true) {
            every { this@mockk.network } returns this@GetCurrencyWarningsUseCaseTest.network
        }
        val currencyStatus = statusFor(token)

        // WHEN
        val result = useCase.invoke(userWalletId, currencyStatus, derivationPath).first()

        // THEN
        assertThat(result).doesNotContain(DynamicAddressesWarnings.FundsFound)
        verify(exactly = 0) {
            dynamicAddressesRepository.hasFundsOnAdditionalAddresses(any(), any())
        }
    }

    @Test
    fun `GIVEN coin currency AND probe emits true WHEN invoke THEN FundsFound is present`() = runTest {
        // GIVEN
        val coin: CryptoCurrency.Coin = mockk(relaxed = true) {
            every { this@mockk.network } returns this@GetCurrencyWarningsUseCaseTest.network
        }
        every { dynamicAddressesRepository.hasFundsOnAdditionalAddresses(userWalletId, network) } returns flowOf(true)
        val currencyStatus = statusFor(coin)

        // WHEN
        val result = useCase.invoke(userWalletId, currencyStatus, derivationPath).first()

        // THEN
        assertThat(result).contains(DynamicAddressesWarnings.FundsFound)
    }

    @Test
    fun `GIVEN coin currency AND probe emits false WHEN invoke THEN FundsFound is absent`() = runTest {
        // GIVEN
        val coin: CryptoCurrency.Coin = mockk(relaxed = true) {
            every { this@mockk.network } returns this@GetCurrencyWarningsUseCaseTest.network
        }
        every { dynamicAddressesRepository.hasFundsOnAdditionalAddresses(userWalletId, network) } returns flowOf(false)
        val currencyStatus = statusFor(coin)

        // WHEN
        val result = useCase.invoke(userWalletId, currencyStatus, derivationPath).first()

        // THEN
        assertThat(result).doesNotContain(DynamicAddressesWarnings.FundsFound)
    }

    @Test
    fun `GIVEN coin balance above existential deposit WHEN invoke THEN ExistentialDeposit warning is absent`() =
        runTest {
            // GIVEN
            val coin = coin()
            val coinStatus = statusFor(currency = coin, amount = BigDecimal("0.98910916"))
            givenCoinStatus(coinStatus)
            coEvery { currencyChecksRepository.getExistentialDeposit(any(), any()) } returns BigDecimal("0.01")

            // WHEN
            val result = useCase.invoke(userWalletId, coinStatus, derivationPath).first()

            // THEN
            assertThat(result.filterIsInstance<CryptoCurrencyWarning.ExistentialDeposit>()).isEmpty()
        }

    @Test
    fun `GIVEN coin balance equal to existential deposit WHEN invoke THEN ExistentialDeposit warning is present`() =
        runTest {
            // GIVEN
            val coin = coin(name = "Polkadot Asset Hub", symbol = "DOT")
            val coinStatus = statusFor(currency = coin, amount = BigDecimal("0.01"))
            givenCoinStatus(coinStatus)
            coEvery { currencyChecksRepository.getExistentialDeposit(any(), any()) } returns BigDecimal("0.01")

            // WHEN
            val result = useCase.invoke(userWalletId, coinStatus, derivationPath).first()

            // THEN
            assertThat(result).contains(
                CryptoCurrencyWarning.ExistentialDeposit(
                    currencyName = "Polkadot Asset Hub",
                    edStringValueWithSymbol = "0.01 DOT",
                ),
            )
        }

    @Test
    fun `GIVEN coin balance below existential deposit WHEN invoke THEN ExistentialDeposit warning is present`() =
        runTest {
            // GIVEN
            val coin = coin()
            val coinStatus = statusFor(currency = coin, amount = BigDecimal("0.005"))
            givenCoinStatus(coinStatus)
            coEvery { currencyChecksRepository.getExistentialDeposit(any(), any()) } returns BigDecimal("0.01")

            // WHEN
            val result = useCase.invoke(userWalletId, coinStatus, derivationPath).first()

            // THEN
            assertThat(result.filterIsInstance<CryptoCurrencyWarning.ExistentialDeposit>()).hasSize(1)
        }

    @Test
    fun `GIVEN unknown coin balance WHEN invoke THEN ExistentialDeposit warning is absent`() = runTest {
        // GIVEN
        val coin = coin()
        val coinStatus = statusFor(currency = coin, amount = null)
        givenCoinStatus(coinStatus)
        coEvery { currencyChecksRepository.getExistentialDeposit(any(), any()) } returns BigDecimal("0.01")

        // WHEN
        val result = useCase.invoke(userWalletId, coinStatus, derivationPath).first()

        // THEN
        assertThat(result.filterIsInstance<CryptoCurrencyWarning.ExistentialDeposit>()).isEmpty()
    }

    @Test
    fun `GIVEN network without existential deposit WHEN invoke THEN ExistentialDeposit warning is absent`() = runTest {
        // GIVEN
        val coin = coin()
        val coinStatus = statusFor(currency = coin, amount = BigDecimal.ZERO)
        givenCoinStatus(coinStatus)
        coEvery { currencyChecksRepository.getExistentialDeposit(any(), any()) } returns null

        // WHEN
        val result = useCase.invoke(userWalletId, coinStatus, derivationPath).first()

        // THEN
        assertThat(result.filterIsInstance<CryptoCurrencyWarning.ExistentialDeposit>()).isEmpty()
    }

    @Test
    fun `GIVEN token with balance AND coin below deposit WHEN invoke THEN warning is built from coin`() = runTest {
        // GIVEN
        val coin = coin(name = "Polkadot Asset Hub", symbol = "DOT")
        val token: CryptoCurrency.Token = mockk(relaxed = true) {
            every { this@mockk.network } returns this@GetCurrencyWarningsUseCaseTest.network
            every { this@mockk.name } returns "Tether"
            every { this@mockk.symbol } returns "USDT"
        }
        val coinStatus = statusFor(currency = coin, amount = BigDecimal("0.005"))
        val tokenStatus = statusFor(currency = token, amount = BigDecimal("100"))
        givenStatuses(coinStatus = coinStatus, currencyStatus = tokenStatus)
        coEvery { currencyChecksRepository.getExistentialDeposit(any(), any()) } returns BigDecimal("0.01")

        // WHEN
        val result = useCase.invoke(userWalletId, tokenStatus, derivationPath).first()

        // THEN
        assertThat(result).contains(
            CryptoCurrencyWarning.ExistentialDeposit(
                currencyName = "Polkadot Asset Hub",
                edStringValueWithSymbol = "0.01 DOT",
            ),
        )
    }

    private fun coin(name: String = "Polkadot", symbol: String = "DOT"): CryptoCurrency.Coin {
        return mockk(relaxed = true) {
            every { this@mockk.network } returns this@GetCurrencyWarningsUseCaseTest.network
            every { this@mockk.name } returns name
            every { this@mockk.symbol } returns symbol
        }
    }

    /** Makes both coin and token lookups resolve to [status], so coin-related warnings are actually built. */
    private fun givenCoinStatus(status: CryptoCurrencyStatus) {
        givenStatuses(coinStatus = status, currencyStatus = status)
    }

    private fun givenStatuses(coinStatus: CryptoCurrencyStatus, currencyStatus: CryptoCurrencyStatus) {
        with(CryptoCurrencyStatusOperations) {
            every { accountStatusList.getCoinStatus(any<CryptoCurrency>()) } returns coinStatus.some()
            every { accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>()) } returns currencyStatus.some()
        }
    }

    private fun statusFor(currency: CryptoCurrency): CryptoCurrencyStatus {
        return mockk(relaxed = true) {
            every { this@mockk.currency } returns currency
        }
    }

    private fun statusFor(currency: CryptoCurrency, amount: BigDecimal?): CryptoCurrencyStatus {
        val statusValue: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
            every { this@mockk.amount } returns amount
        }

        return mockk(relaxed = true) {
            every { this@mockk.currency } returns currency
            every { this@mockk.value } returns statusValue
        }
    }

    private class TestDispatchers(dispatcher: CoroutineDispatcher) : CoroutineDispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val mainImmediate: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val single: CoroutineDispatcher = dispatcher
    }
}