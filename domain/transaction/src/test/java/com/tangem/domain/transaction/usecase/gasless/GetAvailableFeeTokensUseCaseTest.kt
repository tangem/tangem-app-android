package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.AvailableFeeTokens
import com.tangem.domain.transaction.models.tron.TronGaslessToken
import com.tangem.domain.transaction.usecase.gasless.GetAvailableFeeTokensUseCase.Companion.isEligibleFeeToken
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetAvailableFeeTokensUseCaseTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsEligible {

        @ParameterizedTest
        @ProvideTestModels
        fun isEligible(model: EligibilityModel) {
            // Arrange
            val status = createStatus(model.yieldSupplyStatus)

            // Act
            val actual = isEligibleFeeToken(status, isYieldWithdrawEnabled = model.isYieldWithdrawEnabled)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            // Plain token (no yield status) is always eligible, regardless of the toggle.
            EligibilityModel(yieldSupplyStatus = null, isYieldWithdrawEnabled = false, expected = true),
            EligibilityModel(yieldSupplyStatus = null, isYieldWithdrawEnabled = true, expected = true),
            // Active yield: eligible only when gasless v2 (yield withdraw) is enabled.
            EligibilityModel(yieldSupplyStatus = ACTIVE_YIELD, isYieldWithdrawEnabled = true, expected = true),
            EligibilityModel(yieldSupplyStatus = ACTIVE_YIELD, isYieldWithdrawEnabled = false, expected = false),
            // Inactive yield status: excluded either way (no module to withdraw from).
            EligibilityModel(yieldSupplyStatus = INACTIVE_YIELD, isYieldWithdrawEnabled = true, expected = false),
            EligibilityModel(yieldSupplyStatus = INACTIVE_YIELD, isYieldWithdrawEnabled = false, expected = false),
        )

        private fun createStatus(yieldSupplyStatus: YieldSupplyStatus?): CryptoCurrencyStatus {
            val status = mockk<CryptoCurrencyStatus>()
            every { status.value.yieldSupplyStatus } returns yieldSupplyStatus
            return status
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NativeCoinAvailability {

        @BeforeEach
        fun resetMocks() {
            clearMocks(singleAccountStatusListSupplier, gaslessTransactionRepository, currencyChecksRepository)

            coEvery { currencyChecksRepository.isNetworkSupportedForGaslessTx(any()) } returns true
            coEvery { gaslessTransactionRepository.getSupportedTokens(network = any()) } returns setOf(usdc)
        }

        @Test
        fun `GIVEN native balance below the fee WHEN invoke THEN native coin is offered but not enough`() = runTest {
            // Arrange
            givenAccountStatusList(nativeBalance = BigDecimal("0.000001"), tokenBalance = BigDecimal("20"))

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = network,
                sentCurrencyStatus = usdc.status(),
                nativeFeeAmount = NATIVE_FEE,
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(nativeCoin, usdc).inOrder()
            assertThat(actual.notEnoughForFeeIds()).containsExactly(nativeCoin.id)
        }

        @Test
        fun `GIVEN native balance covers the fee WHEN invoke THEN native coin goes first`() = runTest {
            // Arrange
            givenAccountStatusList(nativeBalance = NATIVE_FEE, tokenBalance = BigDecimal("20"))

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = network,
                sentCurrencyStatus = usdc.status(),
                nativeFeeAmount = NATIVE_FEE,
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(nativeCoin, usdc).inOrder()
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }

        @Test
        fun `GIVEN no fee amount known WHEN invoke THEN native coin stays selectable`() = runTest {
            // Arrange
            givenAccountStatusList(nativeBalance = BigDecimal.ZERO, tokenBalance = BigDecimal("20"))

            // Act
            val actual = createUseCase().invoke(userWallet = userWallet, network = network, sentCurrencyStatus = usdc.status())

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(nativeCoin, usdc).inOrder()
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }

        @Test
        fun `GIVEN every token balance is empty WHEN invoke THEN native coin stays selectable`() = runTest {
            // Arrange
            givenAccountStatusList(nativeBalance = BigDecimal.ZERO, tokenBalance = BigDecimal.ZERO)

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = network,
                sentCurrencyStatus = usdc.status(),
                nativeFeeAmount = NATIVE_FEE,
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(nativeCoin, usdc).inOrder()
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }

        @Test
        fun `GIVEN token balance is unknown WHEN invoke THEN native coin stays selectable`() = runTest {
            // Arrange
            givenAccountStatusList(nativeBalance = BigDecimal.ZERO, tokenBalance = null)

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = network,
                sentCurrencyStatus = usdc.status(),
                nativeFeeAmount = NATIVE_FEE,
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(nativeCoin, usdc).inOrder()
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }

        @Test
        fun `GIVEN native balance is unknown WHEN invoke THEN native coin stays selectable`() = runTest {
            // Arrange
            givenAccountStatusList(nativeBalance = null, tokenBalance = BigDecimal("20"))

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = network,
                sentCurrencyStatus = usdc.status(),
                nativeFeeAmount = NATIVE_FEE,
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(nativeCoin, usdc).inOrder()
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }

        @Test
        fun `GIVEN no supported gasless token WHEN invoke THEN native coin is kept as the only option`() = runTest {
            // Arrange
            coEvery { gaslessTransactionRepository.getSupportedTokens(network = any()) } returns emptySet()
            givenAccountStatusList(nativeBalance = BigDecimal.ZERO, tokenBalance = BigDecimal("20"))

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = network,
                sentCurrencyStatus = usdc.status(),
                nativeFeeAmount = NATIVE_FEE,
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(nativeCoin)
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TronGasless {

        @BeforeEach
        fun resetMocks() {
            clearMocks(singleAccountStatusListSupplier, tronGaslessTransactionRepository, walletManagersFacade)

            coEvery { tronGaslessTransactionRepository.getSupportedTokens() } returns listOf(
                TronGaslessToken(contractAddress = USDT_TRON_CONTRACT, symbol = "USDT", decimals = 6),
            )
            coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList(
                listOf(
                    CryptoCurrencyStatus(currency = trx, value = loadedValue(BigDecimal.ZERO)),
                    CryptoCurrencyStatus(currency = usdtTron, value = loadedValue(BigDecimal("20"))),
                    CryptoCurrencyStatus(currency = usdcTron, value = loadedValue(BigDecimal("30"))),
                ),
            )
        }

        @Test
        fun `GIVEN activated tron account WHEN invoke THEN supported token is offered for the fee`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.isTronAccountActivated(userWalletId, tronNetwork) } returns true

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = tronNetwork,
                sentCurrencyStatus = usdtTron.status(),
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(trx, usdtTron).inOrder()
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }

        @Test
        fun `GIVEN a non-gasless token is sent WHEN invoke THEN only native coin is offered`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.isTronAccountActivated(userWalletId, tronNetwork) } returns true

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = tronNetwork,
                sentCurrencyStatus = usdcTron.status(),
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(trx)
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }

        @Test
        fun `GIVEN not activated tron account WHEN invoke THEN only native coin is offered`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.isTronAccountActivated(userWalletId, tronNetwork) } returns false

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = tronNetwork,
                sentCurrencyStatus = usdtTron.status(),
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(trx)
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }

        @Test
        fun `GIVEN a fresher status in the account list WHEN invoke THEN the offered token carries it`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.isTronAccountActivated(userWalletId, tronNetwork) } returns true

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = tronNetwork,
                sentCurrencyStatus = usdtTron.status(BigDecimal.ONE),
            )

            // Assert
            assertThat(actual.offeredAmounts()).containsExactly(BigDecimal.ZERO, BigDecimal("20")).inOrder()
        }

        @Test
        fun `GIVEN the sent token is missing from the account list WHEN invoke THEN the given status is offered`() =
            runTest {
                // Arrange
                coEvery { walletManagersFacade.isTronAccountActivated(userWalletId, tronNetwork) } returns true
                coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList(
                    listOf(CryptoCurrencyStatus(currency = trx, value = loadedValue(BigDecimal.ZERO))),
                )

                // Act
                val actual = createUseCase().invoke(
                    userWallet = userWallet,
                    network = tronNetwork,
                    sentCurrencyStatus = usdtTron.status(BigDecimal("7")),
                )

                // Assert
                assertThat(actual.offeredCurrencies()).containsExactly(trx, usdtTron).inOrder()
                assertThat(actual.offeredAmounts()).containsExactly(BigDecimal.ZERO, BigDecimal("7")).inOrder()
            }

        @Test
        fun `GIVEN the native coin is sent WHEN invoke THEN only native coin is offered`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.isTronAccountActivated(userWalletId, tronNetwork) } returns true

            // Act
            val actual = createUseCase().invoke(
                userWallet = userWallet,
                network = tronNetwork,
                sentCurrencyStatus = trx.status(),
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(trx)
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }
    }

    // region Fixtures

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val tronGaslessTransactionRepository: TronGaslessTransactionRepository = mockk()
    private val currencyChecksRepository: CurrencyChecksRepository = mockk()

    private fun createUseCase() = GetAvailableFeeTokensUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        gaslessTransactionRepository = gaslessTransactionRepository,
        currencyChecksRepository = currencyChecksRepository,
        isTronGaslessSupportedUseCase = IsTronGaslessSupportedUseCase(
            repository = tronGaslessTransactionRepository,
            walletManagersFacade = walletManagersFacade,
        ),
        isYieldWithdrawEnabled = true,
    )

    private fun CryptoCurrency.status(balance: BigDecimal = BigDecimal.TEN) =
        CryptoCurrencyStatus(currency = this, value = loadedValue(balance))

    private fun Either<GetFeeError, AvailableFeeTokens>.offeredCurrencies(): List<CryptoCurrency>? =
        getOrNull()?.tokens?.map { it.currency }

    private fun Either<GetFeeError, AvailableFeeTokens>.offeredAmounts(): List<BigDecimal?>? =
        getOrNull()?.tokens?.map { it.value.amount }

    private fun Either<GetFeeError, AvailableFeeTokens>.notEnoughForFeeIds(): Set<CryptoCurrency.ID>? =
        getOrNull()?.notEnoughForFeeIds

    private fun givenAccountStatusList(nativeBalance: BigDecimal?, tokenBalance: BigDecimal?) {
        val statuses = listOf(
            CryptoCurrencyStatus(currency = nativeCoin, value = valueOf(nativeBalance)),
            CryptoCurrencyStatus(currency = usdc, value = valueOf(tokenBalance)),
        )
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList(statuses)
    }

    /** A `null` balance stands for a status that failed to load. */
    private fun valueOf(balance: BigDecimal?) = balance?.let(::loadedValue) ?: CryptoCurrencyStatus.Unreachable(
        priceChange = null,
        fiatRate = null,
        networkAddress = null,
    )

    private fun accountStatusList(statuses: List<CryptoCurrencyStatus>) = AccountStatusList(
        userWalletId = userWalletId,
        accountStatuses = listOf(
            AccountStatus.CryptoPortfolio(
                account = Account.Personal.createMainAccount(userWalletId),
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = statuses,
                ),
                priceChangeLce = lceLoading(),
            ),
        ),
        totalAccounts = 1,
        totalArchivedAccounts = 0,
        totalFiatBalance = TotalFiatBalance.Loading,
        sortType = TokensSortType.NONE,
        groupType = TokensGroupType.NONE,
    )

    private fun loadedValue(amount: BigDecimal) = CryptoCurrencyStatus.Loaded(
        amount = amount,
        fiatAmount = amount,
        fiatRate = BigDecimal.ONE,
        priceChange = BigDecimal.ZERO,
        stakingBalance = null,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(NetworkAddress.Address(EOA, NetworkAddress.Address.Type.Primary)),
        sources = CryptoCurrencyStatus.Sources(
            networkSource = StatusSource.ACTUAL,
            quoteSource = StatusSource.ACTUAL,
            stakingBalanceSource = StatusSource.ACTUAL,
        ),
    )

    private val userWalletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk<UserWallet.Hot> {
        every { walletId } returns userWalletId
    }

    private val network = Network(
        id = Network.ID(value = "base", derivationPath = Network.DerivationPath.None),
        name = "Base",
        currencySymbol = "ETH",
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = Network.StandardType.Unspecified("Base"),
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )

    private val nativeCoin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "base"),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "ethereum"),
        ),
        network = network,
        name = "Ethereum",
        symbol = "ETH",
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )

    private val usdc = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "base"),
            suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = USDC_CONTRACT),
        ),
        network = network,
        name = "USD Coin",
        symbol = "USDC",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = USDC_CONTRACT,
    )

    private val tronNetwork = network.copy(
        id = Network.ID(value = "tron", derivationPath = Network.DerivationPath.None),
        name = "Tron",
        currencySymbol = "TRX",
        standardType = Network.StandardType.TRC20,
    )

    private val trx = nativeCoin.copy(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "tron"),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "tron"),
        ),
        network = tronNetwork,
        name = "Tron",
        symbol = "TRX",
        decimals = 6,
        displayDecimals = 6,
    )

    private val usdtTron = usdc.copy(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "tron"),
            suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = USDT_TRON_CONTRACT),
        ),
        network = tronNetwork,
        name = "Tether",
        symbol = "USDT",
        contractAddress = USDT_TRON_CONTRACT,
    )

    private val usdcTron = usdc.copy(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "tron"),
            suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = USDC_TRON_CONTRACT),
        ),
        network = tronNetwork,
        contractAddress = USDC_TRON_CONTRACT,
    )

    internal data class EligibilityModel(
        val yieldSupplyStatus: YieldSupplyStatus?,
        val isYieldWithdrawEnabled: Boolean,
        val expected: Boolean,
    )

    // endregion

    private companion object {
        const val USDC_CONTRACT = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913"
        const val USDT_TRON_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        const val USDC_TRON_CONTRACT = "TEkxiTehnzSmSe2XqrBj4w32RUN966rdz8"
        const val EOA = "0xEoa"

        val NATIVE_FEE: BigDecimal = BigDecimal("0.000004")

        val ACTIVE_YIELD = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("100"),
        )
        val INACTIVE_YIELD = ACTIVE_YIELD.copy(isActive = false)
    }
}