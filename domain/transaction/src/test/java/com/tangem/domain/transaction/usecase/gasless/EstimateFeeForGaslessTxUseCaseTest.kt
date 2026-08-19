package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
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
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * [REDACTED_TASK_KEY] — regression coverage for the CEX-swap gasless fee path.
 *
 * The token being swapped sits in a yield module, so the plain EOA balance is zero and the on-chain
 * fee-transfer probe reverts with `InsufficientFundsForOperation`. The use case must still resolve a
 * token fee (topped up by an appended withdraw) instead of raising `NotEnoughFunds` and hiding the fee
 * block — that is what [GetFeeForGaslessUseCase] already does for the Send flow.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EstimateFeeForGaslessTxUseCaseTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val gaslessYieldRepository: GaslessYieldRepository = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val estimateFeeUseCase: EstimateFeeUseCase = mockk()
    private val currencyChecksRepository: CurrencyChecksRepository = mockk()
    private val resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase = mockk()
    private val walletManager: EthereumWalletManager = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            walletManagersFacade,
            gaslessTransactionRepository,
            gaslessYieldRepository,
            singleAccountStatusListSupplier,
            estimateFeeUseCase,
            currencyChecksRepository,
            resolveGaslessFeePlanUseCase,
            walletManager,
        )

        coEvery { currencyChecksRepository.isNetworkSupportedForGaslessTx(any()) } returns true
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId = any(), network = any())
        } returns walletManager
        coEvery {
            walletManagersFacade.estimateFee(amount = any(), userWalletId = any(), network = any())
        } returns Result.Success(TransactionFee.Single(normal = INITIAL_FEE))
        coEvery { gaslessTransactionRepository.getSupportedTokens(network = any()) } returns setOf(usdc)
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns FEE_RECEIVER
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")
        coEvery { gaslessYieldRepository.getYieldContractAddress(any(), any()) } returns MODULE_ADDRESS
        coEvery {
            gaslessYieldRepository.createPartialWithdrawCallData(any(), any(), any())
        } returns mockk<SmartContractCallData>(relaxed = true)
        coEvery { walletManager.getGasLimit(any(), MODULE_ADDRESS, any()) } returns Result.Success(WITHDRAW_GAS)
    }

    @Test
    fun `GIVEN fee token balance is fully inside yield module WHEN estimate THEN token fee with withdraw plan`() =
        runTest {
            // Arrange
            val yieldStatus = activeYieldStatus(effectiveProtocolBalance = SWAP_AMOUNT + BigDecimal("5"))
            givenAccountStatusList(
                nativeBalance = BigDecimal.ZERO,
                tokenBalance = SWAP_AMOUNT + BigDecimal("5"),
                yieldSupplyStatus = yieldStatus,
            )
            // The plain-balance probe reverts: nothing is liquid on the EOA.
            coEvery { walletManager.getGasLimit(any(), FEE_RECEIVER, any()) } returns insufficientFundsFailure()
            val plan = mockk<GaslessFeePlan.TokenPayWithYieldWithdraw>()
            coEvery {
                resolveGaslessFeePlanUseCase(
                    userWallet = any(),
                    tokenStatus = any(),
                    tokenFee = any(),
                    isYieldActive = true,
                    sendAmountInFeeToken = SWAP_AMOUNT,
                )
            } returns Either.Right(plan)

            // Act
            val result = createUseCase(isYieldWithdrawEnabled = true).invoke(
                userWallet = userWallet,
                amount = SWAP_AMOUNT,
                sendingTokenCurrencyStatus = tokenStatus(
                    balance = SWAP_AMOUNT + BigDecimal("5"),
                    yieldSupplyStatus = yieldStatus,
                ),
            )

            // Assert
            val feeExtended = result.getOrNull()
            assertThat(feeExtended).isNotNull()
            assertThat(feeExtended!!.feeTokenId).isEqualTo(usdc.id)
            assertThat(feeExtended.transactionFee.normal).isInstanceOf(Fee.Ethereum.TokenCurrency::class.java)
            assertThat(feeExtended.gaslessFeePlan).isEqualTo(plan)
            assertThat(feeExtended.withdrawGasLimit).isEqualTo(PADDED_WITHDRAW_GAS)
        }

    @Test
    fun `GIVEN the yield module balance is unreadable WHEN estimate THEN falls back to the native fee`() = runTest {
        // Arrange — the resolver refuses to plan a token fee it cannot verify; the block must not disappear.
        val yieldStatus = activeYieldStatus(effectiveProtocolBalance = SWAP_AMOUNT + BigDecimal("5"))
        givenAccountStatusList(
            nativeBalance = BigDecimal.ZERO,
            tokenBalance = SWAP_AMOUNT + BigDecimal("5"),
            yieldSupplyStatus = yieldStatus,
        )
        coEvery { walletManager.getGasLimit(any(), FEE_RECEIVER, any()) } returns insufficientFundsFailure()
        coEvery {
            resolveGaslessFeePlanUseCase(any(), any(), any(), any(), any())
        } returns Either.Left(GetFeeError.GaslessError.YieldBalanceUnavailable)

        // Act
        val result = createUseCase(isYieldWithdrawEnabled = true).invoke(
            userWallet = userWallet,
            amount = SWAP_AMOUNT,
            sendingTokenCurrencyStatus = tokenStatus(
                balance = SWAP_AMOUNT + BigDecimal("5"),
                yieldSupplyStatus = yieldStatus,
            ),
        )

        // Assert
        val feeExtended = result.getOrNull()
        assertThat(feeExtended).isNotNull()
        assertThat(feeExtended!!.feeTokenId).isEqualTo(nativeCoin.id)
        assertThat(feeExtended.gaslessFeePlan).isNull()
    }

    @Test
    fun `GIVEN yield withdraw toggle is off WHEN estimate THEN falls back to the native fee`() = runTest {
        // Arrange
        val yieldStatus = activeYieldStatus(effectiveProtocolBalance = SWAP_AMOUNT + BigDecimal("5"))
        givenAccountStatusList(
            nativeBalance = BigDecimal.ZERO,
            tokenBalance = SWAP_AMOUNT + BigDecimal("5"),
            yieldSupplyStatus = yieldStatus,
        )
        coEvery { walletManager.getGasLimit(any(), FEE_RECEIVER, any()) } returns insufficientFundsFailure()

        // Act
        val result = createUseCase(isYieldWithdrawEnabled = false).invoke(
            userWallet = userWallet,
            amount = SWAP_AMOUNT,
            sendingTokenCurrencyStatus = tokenStatus(
                balance = SWAP_AMOUNT + BigDecimal("5"),
                yieldSupplyStatus = yieldStatus,
            ),
        )

        // Assert — no gasless token fee is possible, but the block must still render a native fee
        val feeExtended = result.getOrNull()
        assertThat(feeExtended).isNotNull()
        assertThat(feeExtended!!.feeTokenId).isEqualTo(nativeCoin.id)
        assertThat(feeExtended.gaslessFeePlan).isNull()
    }

    @Test
    fun `GIVEN liquid token balance without yield WHEN estimate THEN token fee without a plan`() = runTest {
        // Arrange
        givenAccountStatusList(
            nativeBalance = BigDecimal.ZERO,
            tokenBalance = SWAP_AMOUNT + BigDecimal("5"),
            yieldSupplyStatus = null,
        )
        coEvery {
            walletManager.getGasLimit(any(), FEE_RECEIVER, any())
        } returns Result.Success(BigInteger("60000"))

        // Act
        val result = createUseCase(isYieldWithdrawEnabled = true).invoke(
            userWallet = userWallet,
            amount = SWAP_AMOUNT,
            sendingTokenCurrencyStatus = tokenStatus(
                balance = SWAP_AMOUNT + BigDecimal("5"),
                yieldSupplyStatus = null,
            ),
        )

        // Assert — the pre-existing non-yield flow is left untouched: a token fee, no plan, no withdraw gas
        val feeExtended = result.getOrNull()
        assertThat(feeExtended).isNotNull()
        assertThat(feeExtended!!.feeTokenId).isEqualTo(usdc.id)
        assertThat(feeExtended.gaslessFeePlan).isNull()
        assertThat(feeExtended.withdrawGasLimit).isNull()
        coVerify(exactly = 0) {
            resolveGaslessFeePlanUseCase(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `GIVEN native balance covers the fee WHEN estimate THEN native fee is used`() = runTest {
        // Arrange
        givenAccountStatusList(
            nativeBalance = BigDecimal("1"),
            tokenBalance = SWAP_AMOUNT,
            yieldSupplyStatus = null,
        )

        // Act
        val result = createUseCase(isYieldWithdrawEnabled = true).invoke(
            userWallet = userWallet,
            amount = SWAP_AMOUNT,
            sendingTokenCurrencyStatus = tokenStatus(balance = SWAP_AMOUNT, yieldSupplyStatus = null),
        )

        // Assert
        val feeExtended = result.getOrNull()
        assertThat(feeExtended).isNotNull()
        assertThat(feeExtended!!.feeTokenId).isEqualTo(nativeCoin.id)
        assertThat(feeExtended.transactionFee.normal).isEqualTo(INITIAL_FEE)
    }

    // region Fixtures

    private fun createUseCase(isYieldWithdrawEnabled: Boolean) = EstimateFeeForGaslessTxUseCase(
        walletManagersFacade = walletManagersFacade,
        demoConfig = mockk { every { isDemoCardId(any()) } returns false },
        gaslessTransactionRepository = gaslessTransactionRepository,
        gaslessYieldRepository = gaslessYieldRepository,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        estimateFeeUseCase = estimateFeeUseCase,
        currencyChecksRepository = currencyChecksRepository,
        resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
        isYieldWithdrawEnabled = isYieldWithdrawEnabled,
    )

    private fun givenAccountStatusList(
        nativeBalance: BigDecimal,
        tokenBalance: BigDecimal,
        yieldSupplyStatus: YieldSupplyStatus?,
    ) {
        val statuses = listOf(
            coinStatus(balance = nativeBalance),
            tokenStatus(balance = tokenBalance, yieldSupplyStatus = yieldSupplyStatus),
        )
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList(statuses)
    }

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

    private fun coinStatus(balance: BigDecimal) = CryptoCurrencyStatus(
        currency = nativeCoin,
        value = loadedValue(amount = balance, fiatRate = BigDecimal("3000"), yieldSupplyStatus = null),
    )

    private fun tokenStatus(balance: BigDecimal, yieldSupplyStatus: YieldSupplyStatus?) = CryptoCurrencyStatus(
        currency = usdc,
        value = loadedValue(amount = balance, fiatRate = BigDecimal.ONE, yieldSupplyStatus = yieldSupplyStatus),
    )

    private fun loadedValue(
        amount: BigDecimal,
        fiatRate: BigDecimal,
        yieldSupplyStatus: YieldSupplyStatus?,
    ) = CryptoCurrencyStatus.Loaded(
        amount = amount,
        fiatAmount = amount * fiatRate,
        fiatRate = fiatRate,
        priceChange = BigDecimal.ZERO,
        stakingBalance = null,
        yieldSupplyStatus = yieldSupplyStatus,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = NetworkAddress.Single(NetworkAddress.Address(EOA, NetworkAddress.Address.Type.Primary)),
        sources = CryptoCurrencyStatus.Sources(
            networkSource = StatusSource.ACTUAL,
            quoteSource = StatusSource.ACTUAL,
            stakingBalanceSource = StatusSource.ACTUAL,
        ),
    )

    private fun activeYieldStatus(effectiveProtocolBalance: BigDecimal) = YieldSupplyStatus(
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = effectiveProtocolBalance,
    )

    private fun insufficientFundsFailure() = Result.Failure(
        BlockchainSdkError.WrappedThrowable(
            BlockchainSdkError.Ethereum.InsufficientFundsForOperation("insufficient funds for transfer"),
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

    // endregion

    private companion object {
        const val USDC_CONTRACT = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913"
        const val FEE_RECEIVER = "0xFeeReceiver"
        const val MODULE_ADDRESS = "0xModule"
        const val EOA = "0xEoa"

        val SWAP_AMOUNT: BigDecimal = BigDecimal("20.421312")
        val WITHDRAW_GAS: BigInteger = BigInteger("200000")

        /** [WITHDRAW_GAS] after TokenFeeCalculator's PERCENT_TO_INCREASE_SUB_CALL_GASLIMIT safety margin. */
        val PADDED_WITHDRAW_GAS: BigInteger = BigInteger("280000")

        val INITIAL_FEE = Fee.Ethereum.EIP1559(
            amount = com.tangem.blockchain.common.Amount(
                currencySymbol = "ETH",
                value = BigDecimal("0.000004"),
                decimals = 18,
            ),
            gasLimit = BigInteger("337882"),
            maxFeePerGas = BigInteger("7020000"),
            priorityFee = BigInteger("1020000"),
        )
    }
}