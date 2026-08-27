package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
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
 * [REDACTED_TASK_KEY] — the manual fee-token path of a CEX swap (the user explicitly picks which token pays the
 * gasless fee). Two things must hold for a yield-active token: the resolved fee carries a
 * [GaslessFeePlan] (the send needs it to append the withdraw sub-call), and an uncoverable amount
 * degrades to the native fee rather than failing the load and leaving an empty fee block.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EstimateFeeForTokenUseCaseTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val gaslessYieldRepository: GaslessYieldRepository = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
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
        } returns Result.Success(INITIAL_TX_FEE)
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns FEE_RECEIVER
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")
        coEvery { gaslessYieldRepository.getYieldContractAddress(any(), any()) } returns MODULE_ADDRESS
        coEvery {
            gaslessYieldRepository.createPartialWithdrawCallData(any(), any(), any())
        } returns mockk<SmartContractCallData>(relaxed = true)
        coEvery { walletManager.getGasLimit(any(), MODULE_ADDRESS, any()) } returns Result.Success(WITHDRAW_GAS)
        // The plain-balance probe reverts whenever the EOA holds nothing liquid.
        coEvery { walletManager.getGasLimit(any(), FEE_RECEIVER, any()) } returns insufficientFundsFailure()
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList()
    }

    @Test
    fun `GIVEN yield active fee token WHEN estimate THEN plan is attached`() = runTest {
        // Arrange
        val yieldStatus = activeYieldStatus(effectiveProtocolBalance = TOKEN_BALANCE)
        val feeTokenStatus = tokenStatus(balance = TOKEN_BALANCE, yieldSupplyStatus = yieldStatus)
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
            feeTokenCurrencyStatus = feeTokenStatus,
            sendingTokenCurrencyStatus = feeTokenStatus,
            amount = SWAP_AMOUNT,
        )

        // Assert
        val feeExtended = result.getOrNull()
        assertThat(feeExtended).isNotNull()
        assertThat(feeExtended!!.feeTokenId).isEqualTo(usdc.id)
        assertThat(feeExtended.gaslessFeePlan).isEqualTo(plan)
        assertThat(feeExtended.withdrawGasLimit).isEqualTo(PADDED_WITHDRAW_GAS)
    }

    @Test
    fun `GIVEN balance cannot cover send plus fee WHEN estimate THEN falls back to the native fee`() = runTest {
        // Arrange
        val yieldStatus = activeYieldStatus(effectiveProtocolBalance = TOKEN_BALANCE)
        val feeTokenStatus = tokenStatus(balance = TOKEN_BALANCE, yieldSupplyStatus = yieldStatus)
        coEvery {
            resolveGaslessFeePlanUseCase(any(), any(), any(), any(), any())
        } returns Either.Left(GetFeeError.GaslessError.NotEnoughFunds)

        // Act — swapping the whole balance leaves nothing to pay the fee with
        val result = createUseCase(isYieldWithdrawEnabled = true).invoke(
            userWallet = userWallet,
            feeTokenCurrencyStatus = feeTokenStatus,
            sendingTokenCurrencyStatus = feeTokenStatus,
            amount = TOKEN_BALANCE,
        )

        // Assert — a fee is still returned, so the fee block renders instead of disappearing
        val feeExtended = result.getOrNull()
        assertThat(feeExtended).isNotNull()
        assertThat(feeExtended!!.feeTokenId).isEqualTo(nativeCoin.id)
        assertThat(feeExtended.transactionFee).isEqualTo(INITIAL_TX_FEE)
        assertThat(feeExtended.gaslessFeePlan).isNull()
    }

    @Test
    fun `GIVEN the yield module balance is unreadable WHEN estimate THEN falls back to the native fee`() = runTest {
        // Arrange — no token-fee plan can be trusted while the module holding the balance cannot be read.
        val yieldStatus = activeYieldStatus(effectiveProtocolBalance = TOKEN_BALANCE)
        val feeTokenStatus = tokenStatus(balance = TOKEN_BALANCE, yieldSupplyStatus = yieldStatus)
        coEvery {
            resolveGaslessFeePlanUseCase(any(), any(), any(), any(), any())
        } returns Either.Left(GetFeeError.GaslessError.YieldBalanceUnavailable)

        // Act
        val result = createUseCase(isYieldWithdrawEnabled = true).invoke(
            userWallet = userWallet,
            feeTokenCurrencyStatus = feeTokenStatus,
            sendingTokenCurrencyStatus = feeTokenStatus,
            amount = SWAP_AMOUNT,
        )

        // Assert — the fee block must keep rendering instead of failing the whole load
        val feeExtended = result.getOrNull()
        assertThat(feeExtended).isNotNull()
        assertThat(feeExtended!!.feeTokenId).isEqualTo(nativeCoin.id)
        assertThat(feeExtended.transactionFee).isEqualTo(INITIAL_TX_FEE)
        assertThat(feeExtended.gaslessFeePlan).isNull()
    }

    @Test
    fun `GIVEN plan resolution fails for another reason WHEN estimate THEN the error propagates`() = runTest {
        // Arrange
        val yieldStatus = activeYieldStatus(effectiveProtocolBalance = TOKEN_BALANCE)
        val feeTokenStatus = tokenStatus(balance = TOKEN_BALANCE, yieldSupplyStatus = yieldStatus)
        coEvery {
            resolveGaslessFeePlanUseCase(any(), any(), any(), any(), any())
        } returns Either.Left(GetFeeError.GaslessError.ModuleUpdateUnavailable)

        // Act
        val result = createUseCase(isYieldWithdrawEnabled = true).invoke(
            userWallet = userWallet,
            feeTokenCurrencyStatus = feeTokenStatus,
            sendingTokenCurrencyStatus = feeTokenStatus,
            amount = SWAP_AMOUNT,
        )

        // Assert — only NotEnoughFunds degrades to native; everything else must surface
        assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.ModuleUpdateUnavailable)
    }

    @Test
    fun `GIVEN fee token without yield WHEN estimate THEN token fee without a plan`() = runTest {
        // Arrange
        val feeTokenStatus = tokenStatus(balance = TOKEN_BALANCE, yieldSupplyStatus = null)
        coEvery {
            walletManager.getGasLimit(any(), FEE_RECEIVER, any())
        } returns Result.Success(BigInteger("60000"))

        // Act
        val result = createUseCase(isYieldWithdrawEnabled = true).invoke(
            userWallet = userWallet,
            feeTokenCurrencyStatus = feeTokenStatus,
            sendingTokenCurrencyStatus = feeTokenStatus,
            amount = SWAP_AMOUNT,
        )

        // Assert — the pre-existing non-yield flow is untouched
        val feeExtended = result.getOrNull()
        assertThat(feeExtended).isNotNull()
        assertThat(feeExtended!!.feeTokenId).isEqualTo(usdc.id)
        assertThat(feeExtended.gaslessFeePlan).isNull()
        coVerify(exactly = 0) { resolveGaslessFeePlanUseCase(any(), any(), any(), any(), any()) }
    }

    // region Fixtures

    private fun createUseCase(isYieldWithdrawEnabled: Boolean) = EstimateFeeForTokenUseCase(
        gaslessTransactionRepository = gaslessTransactionRepository,
        gaslessYieldRepository = gaslessYieldRepository,
        walletManagersFacade = walletManagersFacade,
        demoConfig = mockk { every { isDemoCardId(any()) } returns false },
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        currencyChecksRepository = currencyChecksRepository,
        resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
        isYieldWithdrawEnabled = isYieldWithdrawEnabled,
    )

    private fun accountStatusList() = AccountStatusList(
        userWalletId = userWalletId,
        accountStatuses = listOf(
            AccountStatus.CryptoPortfolio(
                account = Account.CryptoPortfolio.createMainAccount(userWalletId),
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = listOf(coinStatus(), tokenStatus(TOKEN_BALANCE, null)),
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

    private fun coinStatus() = CryptoCurrencyStatus(
        currency = nativeCoin,
        value = loadedValue(BigDecimal.ZERO, BigDecimal("3000"), null),
    )

    private fun tokenStatus(balance: BigDecimal, yieldSupplyStatus: YieldSupplyStatus?) = CryptoCurrencyStatus(
        currency = usdc,
        value = loadedValue(balance, BigDecimal.ONE, yieldSupplyStatus),
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
        val TOKEN_BALANCE: BigDecimal = BigDecimal("25.421312")
        val WITHDRAW_GAS: BigInteger = BigInteger("200000")

        /** [WITHDRAW_GAS] after TokenFeeCalculator's PERCENT_TO_INCREASE_SUB_CALL_GASLIMIT safety margin. */
        val PADDED_WITHDRAW_GAS: BigInteger = BigInteger("280000")

        val INITIAL_TX_FEE: TransactionFee = TransactionFee.Single(
            normal = Fee.Ethereum.EIP1559(
                amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000004"), decimals = 18),
                gasLimit = BigInteger("337882"),
                maxFeePerGas = BigInteger("7020000"),
                priorityFee = BigInteger("1020000"),
            ),
        )
    }
}