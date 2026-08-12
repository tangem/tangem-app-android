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
import com.tangem.domain.transaction.usecase.gasless.GetAvailableFeeTokensUseCase.Companion.isEligibleFeeToken
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
            val actual = createUseCase().invoke(userWallet = userWallet, network = network)

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
                nativeFeeAmount = NATIVE_FEE,
            )

            // Assert
            assertThat(actual.offeredCurrencies()).containsExactly(nativeCoin)
            assertThat(actual.notEnoughForFeeIds()).isEmpty()
        }
    }

    // region Fixtures

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val tronGaslessTransactionRepository: TronGaslessTransactionRepository = mockk()
    private val currencyChecksRepository: CurrencyChecksRepository = mockk()

    private fun createUseCase() = GetAvailableFeeTokensUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        gaslessTransactionRepository = gaslessTransactionRepository,
        tronGaslessTransactionRepository = tronGaslessTransactionRepository,
        currencyChecksRepository = currencyChecksRepository,
        isYieldWithdrawEnabled = true,
    )

    private fun Either<GetFeeError, AvailableFeeTokens>.offeredCurrencies(): List<CryptoCurrency>? =
        getOrNull()?.tokens?.map { it.currency }

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
                account = Account.CryptoPortfolio.createMainAccount(userWalletId),
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

    internal data class EligibilityModel(
        val yieldSupplyStatus: YieldSupplyStatus?,
        val isYieldWithdrawEnabled: Boolean,
        val expected: Boolean,
    )

    // endregion

    private companion object {
        const val USDC_CONTRACT = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913"
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