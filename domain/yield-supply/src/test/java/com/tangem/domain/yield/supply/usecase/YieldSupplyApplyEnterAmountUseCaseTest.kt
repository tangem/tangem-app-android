package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.factory.EthereumYieldSupplyDeployCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyInitTokenCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyApplyEnterAmountUseCaseTest {

    private val repository: YieldSupplyTransactionRepository = mockk()

    private val useCase = YieldSupplyApplyEnterAmountUseCase(yieldSupplyTransactionRepository = repository)

    private val userWalletId = UserWalletId("abcdef012345")
    private val token = createToken()

    @BeforeEach
    fun setUp() {
        clearMocks(repository)
        coEvery { repository.getYieldModuleVersionStatus(userWalletId, token.network) } returns
            YieldModuleVersionStatus.UpToDate
    }

    @Test
    fun `GIVEN plain enter call last WHEN invoke THEN only enter is replaced with floored liquid minus fee`() =
        runTest {
            // Arrange
            val approve = createTransaction(ApprovalERC20TokenCallData(spenderAddress = MODULE_ADDRESS, amount = null))
            val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
            val status = createTokenStatus(
                total = BigDecimal("12.0000005"),
                yieldSupplyStatus = createYieldStatus(effectiveProtocolBalance = BigDecimal("2")),
            )

            // Act
            val result = invokeUseCase(status = status, calls = listOf(approve, enter), feeAmount = BigDecimal("0.5"))

            // Assert
            assertCalls(result, expected = listOf(approve, enter.withEnterAmount(BigInteger("9500000"))))
            coVerify(exactly = 0) { repository.getEffectiveProtocolBalance(any(), any()) }
            coVerify(exactly = 1) { repository.getYieldModuleVersionStatus(userWalletId, token.network) }
        }

    @ParameterizedTest
    @MethodSource("provideOutdatedVersionStatuses")
    fun `GIVEN plain enter on not up-to-date module WHEN invoke THEN returns left`(
        versionStatus: YieldModuleVersionStatus,
    ) = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(total = BigDecimal("10"), yieldSupplyStatus = createYieldStatus(BigDecimal.ZERO))
        coEvery { repository.getYieldModuleVersionStatus(userWalletId, token.network) } returns versionStatus

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1"))

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN module version check fails WHEN invoke THEN returns left`() = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(total = BigDecimal("10"), yieldSupplyStatus = createYieldStatus(BigDecimal.ZERO))
        coEvery {
            repository.getYieldModuleVersionStatus(userWalletId, token.network)
        } throws IllegalStateException("rpc")

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1"))

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN batch deploys module WHEN invoke THEN enter is replaced without version check`() = runTest {
        // Arrange
        val deploy = createTransaction(
            EthereumYieldSupplyDeployCallData(
                address = WALLET_ADDRESS,
                tokenContractAddress = TOKEN_ADDRESS,
                maxNetworkFee = createAmount(BigDecimal("1")),
            ),
        )
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(total = BigDecimal("10"), yieldSupplyStatus = null)

        // Act
        val result = invokeUseCase(status = status, calls = listOf(deploy, enter), feeAmount = BigDecimal("1"))

        // Assert
        assertCalls(result, expected = listOf(deploy, enter.withEnterAmount(BigInteger("9000000"))))
        coVerify(exactly = 0) { repository.getYieldModuleVersionStatus(any(), any()) }
        coVerify(exactly = 0) { repository.getEffectiveProtocolBalance(any(), any()) }
    }

    @Test
    fun `GIVEN batch registers the token in an existing module WHEN invoke THEN module balance is not read`() =
        runTest {
            // Arrange
            val init = createTransaction(
                EthereumYieldSupplyInitTokenCallData(
                    tokenContractAddress = TOKEN_ADDRESS,
                    maxNetworkFee = createAmount(BigDecimal("1")),
                ),
            )
            val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
            val status = createTokenStatus(total = BigDecimal("10"), yieldSupplyStatus = null)
            coEvery { repository.getEffectiveProtocolBalance(userWalletId, token) } throws
                IllegalStateException("execution reverted")

            // Act
            val result = invokeUseCase(status = status, calls = listOf(init, enter), feeAmount = BigDecimal("1"))

            // Assert
            assertCalls(result, expected = listOf(init, enter.withEnterAmount(BigInteger("9000000"))))
            coVerify(exactly = 0) { repository.getEffectiveProtocolBalance(any(), any()) }
        }

    @Test
    fun `GIVEN batch registers another token WHEN invoke THEN module balance is read`() = runTest {
        // Arrange
        val init = createTransaction(
            EthereumYieldSupplyInitTokenCallData(
                tokenContractAddress = OTHER_TOKEN_ADDRESS,
                maxNetworkFee = createAmount(BigDecimal("1")),
            ),
        )
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(total = BigDecimal("15"), yieldSupplyStatus = null)
        coEvery { repository.getEffectiveProtocolBalance(userWalletId, token) } returns BigDecimal("5")

        // Act
        val result = invokeUseCase(status = status, calls = listOf(init, enter), feeAmount = BigDecimal("1"))

        // Assert
        assertCalls(result, expected = listOf(init, enter.withEnterAmount(BigInteger("9000000"))))
    }

    @Test
    fun `GIVEN batch upgrades module in another call WHEN invoke THEN enter is replaced without version check`() =
        runTest {
            // Arrange
            val upgradedInit = createTransaction(
                EthereumYieldSupplyUpgradeToAndCallCallData(
                    newImplementation = NEW_IMPLEMENTATION,
                    innerCallData = EthereumYieldSupplyInitTokenCallData(
                        tokenContractAddress = TOKEN_ADDRESS,
                        maxNetworkFee = createAmount(BigDecimal("1")),
                    ),
                ),
            )
            val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
            val status = createTokenStatus(
                total = BigDecimal("10"),
                yieldSupplyStatus = createYieldStatus(effectiveProtocolBalance = null, isInitialized = false),
            )

            // Act
            val result = invokeUseCase(
                status = status,
                calls = listOf(upgradedInit, enter),
                feeAmount = BigDecimal("1"),
            )

            // Assert
            assertCalls(result, expected = listOf(upgradedInit, enter.withEnterAmount(BigInteger("9000000"))))
            coVerify(exactly = 0) { repository.getYieldModuleVersionStatus(any(), any()) }
        }

    @Test
    fun `GIVEN upgrade-wrapped enter WHEN invoke THEN wrapper keeps implementation around new enter`() = runTest {
        // Arrange
        val enter = createTransaction(
            EthereumYieldSupplyUpgradeToAndCallCallData(
                newImplementation = NEW_IMPLEMENTATION,
                innerCallData = EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS),
            ),
        )
        val status = createTokenStatus(
            total = BigDecimal("10"),
            yieldSupplyStatus = createYieldStatus(effectiveProtocolBalance = BigDecimal.ZERO),
        )

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1.25"))

        // Assert
        val expected = enter.withCallData(
            EthereumYieldSupplyUpgradeToAndCallCallData(
                newImplementation = NEW_IMPLEMENTATION,
                innerCallData = EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS, amount = BigInteger("8750000")),
            ),
        )
        assertCalls(result, expected = listOf(expected))
        coVerify(exactly = 0) { repository.getYieldModuleVersionStatus(any(), any()) }
    }

    @Test
    fun `GIVEN module balance absent in status WHEN invoke THEN reads it from repository`() = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(total = BigDecimal("15"), yieldSupplyStatus = null)
        coEvery { repository.getEffectiveProtocolBalance(userWalletId, token) } returns BigDecimal("5")

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1"))

        // Assert
        assertCalls(result, expected = listOf(enter.withEnterAmount(BigInteger("9000000"))))
    }

    @Test
    fun `GIVEN uninitialized module without balance WHEN invoke THEN treats module as empty`() = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(
            total = BigDecimal("3"),
            yieldSupplyStatus = createYieldStatus(effectiveProtocolBalance = null, isInitialized = false),
        )

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1"))

        // Assert
        assertCalls(result, expected = listOf(enter.withEnterAmount(BigInteger("2000000"))))
        coVerify(exactly = 0) { repository.getEffectiveProtocolBalance(any(), any()) }
    }

    @Test
    fun `GIVEN module balance read fails WHEN invoke THEN returns left`() = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(total = BigDecimal("15"), yieldSupplyStatus = null)
        coEvery { repository.getEffectiveProtocolBalance(userWalletId, token) } throws IllegalStateException("rpc")

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1"))

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN module balance unavailable WHEN invoke THEN returns left`() = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(total = BigDecimal("15"), yieldSupplyStatus = null)
        coEvery { repository.getEffectiveProtocolBalance(userWalletId, token) } returns null

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1"))

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN fee consumes whole liquid balance WHEN invoke THEN returns left`() = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val status = createTokenStatus(
            total = BigDecimal("5.0000009"),
            yieldSupplyStatus = createYieldStatus(effectiveProtocolBalance = BigDecimal("4")),
        )

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1"))

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN enter call not last WHEN invoke THEN returns left`() = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS))
        val approve = createTransaction(ApprovalERC20TokenCallData(spenderAddress = MODULE_ADDRESS, amount = null))
        val status = createTokenStatus(total = BigDecimal("10"), yieldSupplyStatus = createYieldStatus(BigDecimal.ZERO))

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter, approve), feeAmount = BigDecimal("1"))

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN no calls WHEN invoke THEN returns left`() = runTest {
        // Arrange
        val status = createTokenStatus(total = BigDecimal("10"), yieldSupplyStatus = createYieldStatus(BigDecimal.ZERO))

        // Act
        val result = invokeUseCase(status = status, calls = emptyList(), feeAmount = BigDecimal("1"))

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN enter for another token WHEN invoke THEN returns left`() = runTest {
        // Arrange
        val enter = createTransaction(EthereumYieldSupplyEnterCallData(OTHER_TOKEN_ADDRESS))
        val status = createTokenStatus(total = BigDecimal("10"), yieldSupplyStatus = createYieldStatus(BigDecimal.ZERO))

        // Act
        val result = invokeUseCase(status = status, calls = listOf(enter), feeAmount = BigDecimal("1"))

        // Assert
        assertThat(result.isLeft()).isTrue()
    }

    private suspend fun invokeUseCase(
        status: CryptoCurrencyStatus,
        calls: List<TransactionData.Uncompiled>,
        feeAmount: BigDecimal,
    ) = useCase(userWalletId = userWalletId, cryptoCurrencyStatus = status, calls = calls, feeAmount = feeAmount)

    /** Call data classes have no structural equality, so calls are compared with their call data as encoded bytes. */
    private fun assertCalls(
        actual: Either<Throwable, List<TransactionData.Uncompiled>>,
        expected: List<TransactionData.Uncompiled>,
    ) {
        assertThat(actual.map { calls -> calls.map { it.comparable() } }).isEqualTo(
            expected.map { it.comparable() }.right(),
        )
    }

    private fun TransactionData.Uncompiled.comparable(): Pair<TransactionData.Uncompiled, List<Byte>?> {
        val extras = ethereumExtras()
        return copy(extras = extras.copy(callData = null)) to extras.callData?.data?.toList()
    }

    private fun TransactionData.Uncompiled.withEnterAmount(amount: BigInteger): TransactionData.Uncompiled =
        withCallData(EthereumYieldSupplyEnterCallData(TOKEN_ADDRESS, amount = amount))

    private fun TransactionData.Uncompiled.withCallData(callData: SmartContractCallData): TransactionData.Uncompiled =
        copy(extras = ethereumExtras().copy(callData = callData))

    private fun TransactionData.Uncompiled.ethereumExtras(): EthereumTransactionExtras =
        extras as? EthereumTransactionExtras ?: error("No EVM extras")

    private fun provideOutdatedVersionStatuses(): List<YieldModuleVersionStatus> = listOf(
        YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = OLD_IMPLEMENTATION,
            latestImplementation = NEW_IMPLEMENTATION,
        ),
        YieldModuleVersionStatus.UpgradeUnavailable(currentImplementation = OLD_IMPLEMENTATION),
        YieldModuleVersionStatus.Indeterminate(reason = "rpc"),
        YieldModuleVersionStatus.NotDeployed,
    )

    private fun createAmount(value: BigDecimal): Amount = Amount(
        currencySymbol = "USDC",
        value = value,
        decimals = DECIMALS,
        type = AmountType.Token(Token(symbol = "USDC", contractAddress = TOKEN_ADDRESS, decimals = DECIMALS)),
    )

    private fun createTransaction(callData: SmartContractCallData): TransactionData.Uncompiled {
        val amount = createAmount(BigDecimal.ZERO)
        return TransactionData.Uncompiled(
            amount = amount,
            fee = Fee.Common(amount.copy(value = BigDecimal("0.01"))),
            sourceAddress = WALLET_ADDRESS,
            destinationAddress = MODULE_ADDRESS,
            extras = EthereumTransactionExtras(callData = callData, gasLimit = BigInteger("210000"), nonce = null),
        )
    }

    private fun createYieldStatus(
        effectiveProtocolBalance: BigDecimal?,
        isInitialized: Boolean = true,
    ): YieldSupplyStatus = YieldSupplyStatus(
        isActive = isInitialized,
        isInitialized = isInitialized,
        isAllowedToSpend = true,
        effectiveProtocolBalance = effectiveProtocolBalance,
    )

    private fun createTokenStatus(total: BigDecimal, yieldSupplyStatus: YieldSupplyStatus?): CryptoCurrencyStatus =
        CryptoCurrencyStatus(
            currency = token,
            value = CryptoCurrencyStatus.Custom(
                amount = total,
                fiatAmount = null,
                fiatRate = null,
                priceChange = null,
                yieldSupplyStatus = yieldSupplyStatus,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(
                        value = WALLET_ADDRESS,
                        type = NetworkAddress.Address.Type.Primary,
                    ),
                ),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )

    private fun createToken(): CryptoCurrency.Token {
        val derivationPath = Network.DerivationPath.None
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(NETWORK_ID),
                suffix = CryptoCurrency.ID.Suffix.RawID(NETWORK_ID),
            ),
            network = Network(
                id = Network.ID(value = NETWORK_ID, derivationPath = derivationPath),
                name = NETWORK_ID,
                currencySymbol = "POL",
                derivationPath = derivationPath,
                isTestnet = false,
                standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
                hasFiatFeeRate = true,
                canHandleTokens = true,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
                nameResolvingType = Network.NameResolvingType.NONE,
            ),
            name = "USD Coin",
            symbol = "USDC",
            decimals = DECIMALS,
            iconUrl = null,
            isCustom = false,
            contractAddress = TOKEN_ADDRESS,
        )
    }

    private companion object {
        const val NETWORK_ID = "polygon"
        const val DECIMALS = 6
        const val TOKEN_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
        const val OTHER_TOKEN_ADDRESS = "0xc2132d05d31c914a87c6611c10748aeb04b58e8f"
        const val WALLET_ADDRESS = "0x1111111111111111111111111111111111111111"
        const val MODULE_ADDRESS = "0x2222222222222222222222222222222222222222"
        const val NEW_IMPLEMENTATION = "0x3333333333333333333333333333333333333333"
        const val OLD_IMPLEMENTATION = "0x4444444444444444444444444444444444444444"
    }
}