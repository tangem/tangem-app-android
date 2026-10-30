package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.account.models.AccountStatusList
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
import com.tangem.domain.transaction.GaslessTransactionRepository
import com.tangem.domain.transaction.GaslessYieldRepository
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.GaslessApproval
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/** Covers the integrated-approve paths: the approve either joins the gasless batch or stays a native transaction. */
internal class EvmGaslessFeeResolverTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val gaslessYieldRepository: GaslessYieldRepository = mockk()
    private val resolveGaslessFeePlanUseCase: ResolveGaslessFeePlanUseCase = mockk()
    private val walletManager: EthereumWalletManager = mockk()

    private val sut = EvmGaslessFeeResolver(
        walletManagersFacade = walletManagersFacade,
        demoConfig = mockk { every { isDemoCardId(any()) } returns false },
        gaslessTransactionRepository = gaslessTransactionRepository,
        gaslessYieldRepository = gaslessYieldRepository,
        resolveGaslessFeePlanUseCase = resolveGaslessFeePlanUseCase,
    )

    private val swapTransactionData: TransactionData = mockk()
    private val approval = GaslessApproval(
        spenderAddress = SPENDER,
        approvalTransaction = TransactionData.Uncompiled(
            amount = Amount(currencySymbol = "USDC", value = BigDecimal.ONE, decimals = 6),
            fee = null,
            sourceAddress = EOA,
            destinationAddress = USDC_CONTRACT,
            extras = EthereumTransactionExtras(callData = approveCallData()),
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            walletManagersFacade,
            gaslessTransactionRepository,
            gaslessYieldRepository,
            resolveGaslessFeePlanUseCase,
            walletManager,
        )
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId = any(), network = any())
        } returns walletManager
        coEvery {
            walletManager.estimateFeeWithOverride(swapTransactionData, SPENDER, isSimulate = true)
        } returns Result.Success(SWAP_FEE)
        coEvery { walletManager.getFee(approval.approvalTransaction) } returns Result.Success(APPROVE_FEE)
        coEvery { walletManager.getGasLimit(any(), FEE_RECEIVER, any()) } returns Result.Success(BigInteger("60000"))
        coEvery { walletManager.getGasLimit(any(), USDC_CONTRACT, any()) } returns Result.Success(APPROVE_GAS)
        coEvery { gaslessTransactionRepository.getSupportedTokens(network = any()) } returns setOf(usdc)
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns FEE_RECEIVER
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveWithAutoFeeToken {

        @Test
        fun `GIVEN approval and native balance covers swap and approve WHEN resolve THEN native fee`() = runTest {
            // Arrange
            val nativeStatus = coinStatus(balance = BigDecimal("1"))
            val accountStatusList = accountStatusList(nativeStatus, tokenStatus(yieldSupplyStatus = null))

            // Act
            val result = sut.resolveWithAutoFeeToken(
                userWallet = userWallet,
                network = network,
                transactionData = swapTransactionData,
                nativeCurrencyStatus = nativeStatus,
                accountStatusList = accountStatusList,
                nativeTransactionData = swapTransactionData,
                approval = approval,
            )

            // Assert
            val feeExtended = result.getOrNull()!!
            assertThat(feeExtended.feeTokenId).isEqualTo(nativeCoin.id)
            assertThat(feeExtended.transactionFee).isEqualTo(SWAP_FEE)
            assertThat(feeExtended.nativeFee).isEqualTo(SWAP_FEE)
            assertThat(feeExtended.gaslessFeePlan).isNull()
        }

        @Test
        fun `GIVEN approval and native balance covers swap but not approve WHEN resolve THEN token fee with approve plan`() =
            runTest {
                // Arrange — 0.0012 ETH covers the 0.001 swap fee but not the extra 0.0005 approve fee
                val nativeStatus = coinStatus(balance = BigDecimal("0.0012"))
                val accountStatusList = accountStatusList(nativeStatus, tokenStatus(yieldSupplyStatus = null))

                // Act
                val result = sut.resolveWithAutoFeeToken(
                    userWallet = userWallet,
                    network = network,
                    transactionData = swapTransactionData,
                    nativeCurrencyStatus = nativeStatus,
                    accountStatusList = accountStatusList,
                    nativeTransactionData = swapTransactionData,
                    approval = approval,
                )

                // Assert
                val feeExtended = result.getOrNull()!!
                assertThat(feeExtended.feeTokenId).isEqualTo(usdc.id)
                assertThat(feeExtended.nativeFee).isEqualTo(SWAP_FEE)
                val plan = feeExtended.gaslessFeePlan as GaslessFeePlan.TokenPayWithApproval
                assertThat(plan.approval).isEqualTo(approval)
                assertThat(plan.approveGasLimit).isEqualTo(PADDED_APPROVE_GAS)
                coVerify { resolveGaslessFeePlanUseCase wasNot Called }
            }

        @Test
        fun `GIVEN approval and the only fee token has active yield WHEN resolve THEN no supported tokens found`() =
            runTest {
                // Arrange
                val nativeStatus = coinStatus(balance = BigDecimal.ZERO)
                val accountStatusList = accountStatusList(nativeStatus, tokenStatus(yieldSupplyStatus = activeYield))

                // Act
                val result = sut.resolveWithAutoFeeToken(
                    userWallet = userWallet,
                    network = network,
                    transactionData = swapTransactionData,
                    nativeCurrencyStatus = nativeStatus,
                    accountStatusList = accountStatusList,
                    nativeTransactionData = swapTransactionData,
                    approval = approval,
                )

                // Assert
                assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.NoSupportedTokensFound)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveWithSelectedFeeToken {

        @Test
        fun `GIVEN approval WHEN resolve THEN token fee with approve plan estimated with allowance override`() =
            runTest {
                // Arrange
                val tokenStatus = tokenStatus(yieldSupplyStatus = null)

                // Act
                val result = sut.resolveWithSelectedFeeToken(
                    userWallet = userWallet,
                    network = network,
                    transactionData = swapTransactionData,
                    nativeCurrencyStatus = coinStatus(balance = BigDecimal.ZERO),
                    tokenCurrencyStatus = tokenStatus,
                    nativeTransactionData = swapTransactionData,
                    approval = approval,
                )

                // Assert
                val feeExtended = result.getOrNull()!!
                assertThat(feeExtended.feeTokenId).isEqualTo(usdc.id)
                assertThat(feeExtended.nativeFee).isEqualTo(SWAP_FEE)
                val plan = feeExtended.gaslessFeePlan as GaslessFeePlan.TokenPayWithApproval
                assertThat(plan.approval).isEqualTo(approval)
                assertThat(plan.approveGasLimit).isEqualTo(PADDED_APPROVE_GAS)
                coVerify(exactly = 1) {
                    walletManager.estimateFeeWithOverride(swapTransactionData, SPENDER, isSimulate = true)
                }
            }

        @Test
        fun `GIVEN approval and fee token with active yield WHEN resolve THEN network is not supported`() = runTest {
            // Act
            val result = sut.resolveWithSelectedFeeToken(
                userWallet = userWallet,
                network = network,
                transactionData = swapTransactionData,
                nativeCurrencyStatus = coinStatus(balance = BigDecimal.ZERO),
                tokenCurrencyStatus = tokenStatus(yieldSupplyStatus = activeYield),
                nativeTransactionData = swapTransactionData,
                approval = approval,
            )

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(GetFeeError.GaslessError.NetworkIsNotSupported)
            coVerify(exactly = 0) { walletManager.estimateFeeWithOverride(any(), any(), any()) }
        }
    }

    // region Fixtures

    private fun approveCallData() = object : SmartContractCallData {
        override val methodId = "0x095ea7b3"
        override val data = byteArrayOf(0x09, 0x5e)
        override fun validate(blockchain: com.tangem.blockchain.common.Blockchain) = true
    }

    private val activeYield = YieldSupplyStatus(
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = BigDecimal("100"),
    )

    private fun accountStatusList(vararg statuses: CryptoCurrencyStatus) = AccountStatusList(
        userWalletId = userWalletId,
        accountStatuses = listOf(
            AccountStatus.Personal(
                account = Account.Personal.createMainAccount(userWalletId),
                tokenList = TokenList.Ungrouped(
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                    currencies = statuses.toList(),
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

    private fun tokenStatus(yieldSupplyStatus: YieldSupplyStatus?) = CryptoCurrencyStatus(
        currency = usdc,
        value = loadedValue(amount = BigDecimal("100"), fiatRate = BigDecimal.ONE, yieldSupplyStatus = yieldSupplyStatus),
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
        const val SPENDER = "0xRouter"
        const val EOA = "0xEoa"

        val APPROVE_GAS: BigInteger = BigInteger("46000")

        /** [APPROVE_GAS] after TokenFeeCalculator's 40% sub-call safety margin. */
        val PADDED_APPROVE_GAS: BigInteger = BigInteger("64400")

        val SWAP_FEE = TransactionFee.Single(normal = ethFee(value = "0.001", gasLimit = "100000"))
        val APPROVE_FEE = TransactionFee.Single(normal = ethFee(value = "0.0005", gasLimit = "46000"))

        fun ethFee(value: String, gasLimit: String) = Fee.Ethereum.EIP1559(
            amount = Amount(currencySymbol = "ETH", value = BigDecimal(value), decimals = 18),
            gasLimit = BigInteger(gasLimit),
            maxFeePerGas = BigInteger("10000000000"),
            priorityFee = BigInteger("1000000000"),
        )
    }
}