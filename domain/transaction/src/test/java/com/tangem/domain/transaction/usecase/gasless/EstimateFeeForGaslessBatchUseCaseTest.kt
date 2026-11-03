package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
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
import com.tangem.domain.transaction.models.YieldModuleSameTokenRule
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

internal class EstimateFeeForGaslessBatchUseCaseTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val gaslessYieldRepository: GaslessYieldRepository = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk()
    private val currencyChecksRepository: CurrencyChecksRepository = mockk()
    private val walletManager: EthereumWalletManager = mockk()
    private val withdrawCallData: SmartContractCallData = mockk(relaxed = true)

    private val useCase = EstimateFeeForGaslessBatchUseCase(
        walletManagersFacade = walletManagersFacade,
        demoConfig = mockk { every { isDemoCardId(any()) } returns false },
        gaslessTransactionRepository = gaslessTransactionRepository,
        gaslessYieldRepository = gaslessYieldRepository,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
        currencyChecksRepository = currencyChecksRepository,
        resolveGaslessFeePlanUseCase = ResolveGaslessFeePlanUseCase(gaslessYieldRepository),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            walletManagersFacade,
            gaslessTransactionRepository,
            gaslessYieldRepository,
            singleAccountStatusListSupplier,
            nonPortfolioCurrenciesProvider,
            currencyChecksRepository,
            walletManager,
        )

        coEvery { currencyChecksRepository.isNetworkSupportedForGaslessTx(any()) } returns true
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId = any(), network = any())
        } returns walletManager
        coEvery { gaslessTransactionRepository.getSupportedTokens(network = any()) } returns setOf(usdc, usdt)
        coEvery { gaslessTransactionRepository.getTokenFeeReceiverAddress() } returns FEE_RECEIVER
        every { gaslessTransactionRepository.getBaseGasForTransaction() } returns BigInteger("21000")
        coEvery {
            walletManager.getGasLimit(any(), FEE_RECEIVER, any())
        } returns Result.Success(BigInteger("60000"))
        coEvery { gaslessYieldRepository.getYieldContractAddress(any(), any()) } returns MODULE_ADDRESS
        coEvery { gaslessYieldRepository.getEffectiveProtocolBalance(any(), any()) } returns BigDecimal.ZERO
        coEvery {
            gaslessYieldRepository.createPartialWithdrawCallData(any(), any(), any())
        } returns withdrawCallData
        coEvery { walletManager.getGasLimit(any(), MODULE_ADDRESS, any()) } returns Result.Success(WITHDRAW_GAS)
    }

    @Test
    fun `GIVEN network without gasless WHEN invoke THEN returns null`() = runTest {
        // Arrange
        coEvery { currencyChecksRepository.isNetworkSupportedForGaslessTx(any()) } returns false

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = usdcStatus(balance = BigDecimal("100")),
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
        )

        // Assert
        assertThat(result).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN native balance covers the summed fee WHEN invoke THEN native fee of all calls`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatus(balance = BigDecimal("100"))
        givenAccountStatusList(coinStatus(balance = BigDecimal("1")), operationTokenStatus)

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
        )

        // Assert
        assertThat(result).isEqualTo(nativeFeeExtended(enterNativeFee()).right())
    }

    @Test
    fun `GIVEN calls with mixed fee types WHEN invoke THEN unknown error`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatus(balance = BigDecimal("100"))
        givenAccountStatusList(coinStatus(balance = BigDecimal("1")), operationTokenStatus)
        val legacyCall = call(
            Fee.Ethereum.Legacy(
                amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000001"), decimals = 18),
                gasLimit = BigInteger("100000"),
                gasPrice = MAX_FEE_PER_GAS,
            ),
        )

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = listOf(legacyCall) + enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
        )

        // Assert
        assertThat(result).isEqualTo(GetFeeError.UnknownError.left())
    }

    @Test
    fun `GIVEN Enter and only the operation token covers the fee WHEN invoke THEN token pay in it`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatus(balance = BigDecimal("100"))
        givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
        )

        // Assert
        assertThat(result).isEqualTo(tokenFeeExtended(feeToken = usdc, tokenFee = result.tokenFee()).right())
    }

    @Test
    fun `GIVEN Enter and the operation token liquid balance is below the fee WHEN invoke THEN native fee`() =
        runTest {
            // Arrange — the total is large, but almost all of it is still held by an inactive module.
            val operationTokenStatus = usdcStatus(
                balance = BigDecimal("100"),
                yieldSupplyStatus = YieldSupplyStatus(
                    isActive = false,
                    isInitialized = true,
                    isAllowedToSpend = true,
                    effectiveProtocolBalance = BigDecimal("99.999999"),
                ),
            )
            givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)

            // Act
            val result = useCase(
                userWallet = userWallet,
                operationTokenStatus = operationTokenStatus,
                calls = enterCalls(),
                sameTokenRule = YieldModuleSameTokenRule.Enter,
            )

            // Assert
            assertThat(result).isEqualTo(nativeFeeExtended(enterNativeFee()).right())
        }

    @Test
    fun `GIVEN Enter and liquid equals the fee WHEN invoke THEN native fee`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatusWithLiquid(liquid = enterTokenFeeAmount())
        givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
        )

        // Assert
        assertThat(result).isEqualTo(nativeFeeExtended(enterNativeFee()).right())
    }

    @Test
    fun `GIVEN Enter and liquid is one minimal unit above the fee WHEN invoke THEN token pay in it`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatusWithLiquid(liquid = enterTokenFeeAmount() + BigDecimal("0.000001"))
        givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
        )

        // Assert
        assertThat(result).isEqualTo(tokenFeeExtended(feeToken = usdc, tokenFee = result.tokenFee()).right())
    }

    @Test
    fun `GIVEN Enter and the operation token is short but another token pays WHEN invoke THEN pays in the other`() =
        runTest {
            // Arrange
            val operationTokenStatus = usdcStatusWithLiquid(liquid = BigDecimal("0.000001"))
            val usdtStatus = usdtStatus(balance = BigDecimal("10"), yieldSupplyStatus = null)
            givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus, usdtStatus)

            // Act
            val result = useCase(
                userWallet = userWallet,
                operationTokenStatus = operationTokenStatus,
                calls = enterCalls(),
                sameTokenRule = YieldModuleSameTokenRule.Enter,
            )

            // Assert
            val expected = tokenFeeExtended(feeToken = usdt, tokenFee = result.tokenFee(), gaslessFeePlan = null)
            assertThat(result).isEqualTo(expected.right())
        }

    @Test
    fun `GIVEN Exit and the operation token is all in the module WHEN invoke THEN token pay without withdraw`() =
        runTest {
            // Arrange — nothing liquid on the EOA: the fee-transfer probe reverts until exit runs.
            val operationTokenStatus = usdcStatus(
                balance = BigDecimal("100"),
                yieldSupplyStatus = activeYieldStatus(effectiveProtocolBalance = BigDecimal("100")),
            )
            givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)
            coEvery { walletManager.getGasLimit(any(), FEE_RECEIVER, any()) } returns revertedEstimateFailure()

            // Act
            val result = useCase(
                userWallet = userWallet,
                operationTokenStatus = operationTokenStatus,
                calls = exitCalls(),
                sameTokenRule = YieldModuleSameTokenRule.Exit,
            )

            // Assert
            val expected = tokenFeeExtended(
                feeToken = usdc,
                tokenFee = result.tokenFee(),
                callGasLimits = EXIT_CALL_GAS_LIMITS,
                nativeFee = exitNativeFee(),
            )
            assertThat(result).isEqualTo(expected.right())
            coVerify(exactly = 0) { gaslessYieldRepository.createPartialWithdrawCallData(any(), any(), any()) }
        }

    @Test
    fun `GIVEN Exit and the operation token total is below the fee WHEN invoke THEN native fee`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatus(
            balance = BigDecimal("0.000001"),
            yieldSupplyStatus = activeYieldStatus(effectiveProtocolBalance = BigDecimal("0.000001")),
        )
        givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)
        coEvery { walletManager.getGasLimit(any(), FEE_RECEIVER, any()) } returns revertedEstimateFailure()

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = exitCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Exit,
        )

        // Assert
        assertThat(result).isEqualTo(nativeFeeExtended(exitNativeFee()).right())
    }

    @Test
    fun `GIVEN another token with active yield is the largest WHEN invoke THEN token pay with yield withdraw`() =
        runTest {
            // Arrange
            val operationTokenStatus = usdcStatus(balance = BigDecimal("1"))
            val usdtStatus = usdtStatus(
                balance = BigDecimal("50"),
                yieldSupplyStatus = activeYieldStatus(effectiveProtocolBalance = BigDecimal("50")),
            )
            givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus, usdtStatus)
            coEvery { gaslessYieldRepository.getEffectiveProtocolBalance(any(), usdt) } returns BigDecimal("50")

            // Act
            val result = useCase(
                userWallet = userWallet,
                operationTokenStatus = operationTokenStatus,
                calls = enterCalls(),
                sameTokenRule = YieldModuleSameTokenRule.Enter,
            )

            // Assert
            val tokenFee = result.tokenFee()
            val expected = tokenFeeExtended(
                feeToken = usdt,
                tokenFee = tokenFee,
                gaslessFeePlan = GaslessFeePlan.TokenPayWithYieldWithdraw(
                    feeToken = usdt,
                    fee = tokenFee,
                    withdrawAmount = checkNotNull(tokenFee.amount.value).movePointRight(usdt.decimals)
                        .setScale(0, RoundingMode.CEILING)
                        .toBigInteger(),
                    withdrawCallData = withdrawCallData,
                    yieldModuleAddress = MODULE_ADDRESS,
                ),
                withdrawGasLimit = BigInteger("280000"),
            )
            assertThat(result).isEqualTo(expected.right())
        }

    @Test
    fun `GIVEN operation token excluded and another token pays WHEN invoke THEN pays in the other`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatus(balance = BigDecimal("100"))
        val usdtStatus = usdtStatus(balance = BigDecimal("10"), yieldSupplyStatus = null)
        givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus, usdtStatus)

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
            isOperationTokenExcluded = true,
        )

        // Assert
        val expected = tokenFeeExtended(feeToken = usdt, tokenFee = result.tokenFee(), gaslessFeePlan = null)
        assertThat(result).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN operation token excluded and no other token WHEN invoke THEN native fee`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatus(balance = BigDecimal("100"))
        givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
            isOperationTokenExcluded = true,
        )

        // Assert
        assertThat(result).isEqualTo(nativeFeeExtended(enterNativeFee()).right())
    }

    @Test
    fun `GIVEN calls with different gas prices WHEN native covers THEN summed fee takes the max of each price`() =
        runTest {
            // Arrange
            val operationTokenStatus = usdcStatus(balance = BigDecimal("100"))
            givenAccountStatusList(coinStatus(balance = BigDecimal("1")), operationTokenStatus)

            // Act
            val result = useCase(
                userWallet = userWallet,
                operationTokenStatus = operationTokenStatus,
                calls = differentPriceCalls(),
                sameTokenRule = YieldModuleSameTokenRule.Enter,
            )

            // Assert
            val summedFee = TransactionFee.Single(
                normal = Fee.Ethereum.EIP1559(
                    amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.0000035"), decimals = 18),
                    gasLimit = BigInteger("350000"),
                    maxFeePerGas = HIGH_MAX_FEE_PER_GAS,
                    priorityFee = HIGH_PRIORITY_FEE,
                ),
            )
            assertThat(result.getOrNull()).isEqualTo(
                TransactionFeeExtended(transactionFee = summedFee, feeTokenId = nativeCoin.id, nativeFee = summedFee),
            )
        }

    @Test
    fun `GIVEN calls with different gas prices WHEN a token pays THEN its fee is priced at the max gas price`() =
        runTest {
            // Arrange
            val operationTokenStatus = usdcStatus(balance = BigDecimal("100"))
            givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)
            val callsAtMaxPrice = differentPriceCalls().map { call ->
                val fee = call.fee as Fee.Ethereum.EIP1559
                call(fee.copy(maxFeePerGas = HIGH_MAX_FEE_PER_GAS, priorityFee = HIGH_PRIORITY_FEE))
            }
            val expectedTokenFee = useCase(
                userWallet = userWallet,
                operationTokenStatus = operationTokenStatus,
                calls = callsAtMaxPrice,
                sameTokenRule = YieldModuleSameTokenRule.Enter,
            ).tokenFee()

            // Act
            val result = useCase(
                userWallet = userWallet,
                operationTokenStatus = operationTokenStatus,
                calls = differentPriceCalls(),
                sameTokenRule = YieldModuleSameTokenRule.Enter,
            )

            // Assert
            assertThat(result.tokenFee()).isEqualTo(expectedTokenFee)
        }

    @Test
    fun `GIVEN no supported tokens WHEN invoke THEN native fee`() = runTest {
        // Arrange
        val operationTokenStatus = usdcStatus(balance = BigDecimal("100"))
        givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), operationTokenStatus)
        coEvery { gaslessTransactionRepository.getSupportedTokens(network = any()) } returns emptySet()

        // Act
        val result = useCase(
            userWallet = userWallet,
            operationTokenStatus = operationTokenStatus,
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
        )

        // Assert
        assertThat(result).isEqualTo(nativeFeeExtended(enterNativeFee()).right())
    }

    // region fixtures

    private fun enterNativeFee() = TransactionFee.Single(
        normal = callFee(gasLimit = BigInteger("450000"), amount = BigDecimal("0.0000045")),
    )

    private fun exitNativeFee() = TransactionFee.Single(
        normal = callFee(gasLimit = BigInteger("350000"), amount = BigDecimal("0.0000035")),
    )

    private fun nativeFeeExtended(fee: TransactionFee) = TransactionFeeExtended(
        transactionFee = fee,
        feeTokenId = nativeCoin.id,
        nativeFee = fee,
    )

    @Suppress("LongParameterList")
    private fun tokenFeeExtended(
        feeToken: CryptoCurrency.Token,
        tokenFee: Fee.Ethereum.TokenCurrency,
        gaslessFeePlan: GaslessFeePlan? = GaslessFeePlan.TokenPay(feeToken = feeToken, fee = tokenFee),
        callGasLimits: List<BigInteger> = ENTER_CALL_GAS_LIMITS,
        withdrawGasLimit: BigInteger? = null,
        nativeFee: TransactionFee = enterNativeFee(),
    ) = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = tokenFee),
        feeTokenId = feeToken.id,
        gaslessFeePlan = gaslessFeePlan,
        callGasLimits = callGasLimits,
        withdrawGasLimit = withdrawGasLimit,
        gaslessProtocolData = GaslessProtocolData.Evm,
        nativeFee = nativeFee,
    )

    /** The token fee comes from the gas and price estimates, so it is read off the result; the rest is checked. */
    private fun Either<GetFeeError, TransactionFeeExtended?>.tokenFee(): Fee.Ethereum.TokenCurrency {
        return checkNotNull(getOrNull()).transactionFee.normal as Fee.Ethereum.TokenCurrency
    }

    private fun enterCalls() = listOf(
        call(callFee(gasLimit = BigInteger("200000"), amount = BigDecimal("0.000002"))),
        call(callFee(gasLimit = BigInteger("150000"), amount = BigDecimal("0.0000015"))),
        call(callFee(gasLimit = BigInteger("100000"), amount = BigDecimal("0.000001"))),
    )

    private fun exitCalls() = listOf(
        call(callFee(gasLimit = BigInteger("200000"), amount = BigDecimal("0.000002"))),
        call(callFee(gasLimit = BigInteger("150000"), amount = BigDecimal("0.0000015"))),
    )

    /** The second call has the higher max fee, the first the higher priority fee. */
    private fun differentPriceCalls() = listOf(
        call(
            Fee.Ethereum.EIP1559(
                amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.000002"), decimals = 18),
                gasLimit = BigInteger("200000"),
                maxFeePerGas = MAX_FEE_PER_GAS,
                priorityFee = HIGH_PRIORITY_FEE,
            ),
        ),
        call(
            Fee.Ethereum.EIP1559(
                amount = Amount(currencySymbol = "ETH", value = BigDecimal("0.0000015"), decimals = 18),
                gasLimit = BigInteger("150000"),
                maxFeePerGas = HIGH_MAX_FEE_PER_GAS,
                priorityFee = BigInteger("1020000"),
            ),
        ),
    )

    private fun callFee(gasLimit: BigInteger, amount: BigDecimal) = Fee.Ethereum.EIP1559(
        amount = Amount(currencySymbol = "ETH", value = amount, decimals = 18),
        gasLimit = gasLimit,
        maxFeePerGas = MAX_FEE_PER_GAS,
        priorityFee = BigInteger("1020000"),
    )

    private fun call(fee: Fee) = TransactionData.Uncompiled(
        amount = Amount(currencySymbol = "ETH", value = BigDecimal.ZERO, decimals = 18),
        fee = fee,
        sourceAddress = EOA,
        destinationAddress = MODULE_ADDRESS,
    )

    private fun givenAccountStatusList(vararg statuses: CryptoCurrencyStatus) {
        coEvery {
            singleAccountStatusListSupplier.getSyncOrNull(userWalletId)
        } returns accountStatusList(statuses.toList())
    }

    private fun accountStatusList(statuses: List<CryptoCurrencyStatus>) = AccountStatusList(
        userWalletId = userWalletId,
        accountStatuses = listOf(
            AccountStatus.Personal(
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

    private fun coinStatus(balance: BigDecimal) = CryptoCurrencyStatus(
        currency = nativeCoin,
        value = loadedValue(amount = balance, fiatRate = BigDecimal("3000"), yieldSupplyStatus = null),
    )

    private fun usdcStatus(balance: BigDecimal, yieldSupplyStatus: YieldSupplyStatus? = notInitializedYield()) =
        CryptoCurrencyStatus(
            currency = usdc,
            value = loadedValue(amount = balance, fiatRate = BigDecimal.ONE, yieldSupplyStatus = yieldSupplyStatus),
        )

    /** Total of 100 with the rest held by an inactive module, so only [liquid] can pay. */
    private fun usdcStatusWithLiquid(liquid: BigDecimal) = usdcStatus(
        balance = TOTAL_BALANCE,
        yieldSupplyStatus = YieldSupplyStatus(
            isActive = false,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = TOTAL_BALANCE - liquid,
        ),
    )

    /** The token fee does not depend on the balance, so a run with plenty of liquid measures it. */
    private suspend fun enterTokenFeeAmount(): BigDecimal {
        val status = usdcStatus(balance = TOTAL_BALANCE)
        givenAccountStatusList(coinStatus(balance = BigDecimal.ZERO), status)
        val fee = useCase(
            userWallet = userWallet,
            operationTokenStatus = status,
            calls = enterCalls(),
            sameTokenRule = YieldModuleSameTokenRule.Enter,
        ).tokenFee()
        return checkNotNull(fee.amount.value)
    }

    private fun usdtStatus(balance: BigDecimal, yieldSupplyStatus: YieldSupplyStatus?) = CryptoCurrencyStatus(
        currency = usdt,
        value = loadedValue(amount = balance, fiatRate = BigDecimal.ONE, yieldSupplyStatus = yieldSupplyStatus),
    )

    private fun loadedValue(amount: BigDecimal, fiatRate: BigDecimal, yieldSupplyStatus: YieldSupplyStatus?) =
        CryptoCurrencyStatus.Loaded(
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

    private fun notInitializedYield() = YieldSupplyStatus(
        isActive = false,
        isInitialized = false,
        isAllowedToSpend = false,
        effectiveProtocolBalance = null,
    )

    private fun activeYieldStatus(effectiveProtocolBalance: BigDecimal) = YieldSupplyStatus(
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = effectiveProtocolBalance,
    )

    private fun revertedEstimateFailure() = Result.Failure(
        BlockchainSdkError.WrappedThrowable(
            BlockchainSdkError.Ethereum.ExecutionReverted(code = 3, message = "execution reverted"),
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

    private val usdc = token(contract = USDC_CONTRACT, symbol = "USDC")
    private val usdt = token(contract = USDT_CONTRACT, symbol = "USDT")

    private fun token(contract: String, symbol: String) = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawId = "base"),
            suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = contract),
        ),
        network = network,
        name = symbol,
        symbol = symbol,
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = contract,
    )

    // endregion

    private companion object {
        const val USDC_CONTRACT = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913"
        const val USDT_CONTRACT = "0xfde4c96c8593536e31f229ea8f37b2ada2699bb2"
        const val FEE_RECEIVER = "0xFeeReceiver"
        const val MODULE_ADDRESS = "0xModule"
        const val EOA = "0xEoa"

        val TOTAL_BALANCE: BigDecimal = BigDecimal("100")
        val MAX_FEE_PER_GAS: BigInteger = BigInteger("7020000")
        val HIGH_MAX_FEE_PER_GAS: BigInteger = BigInteger("9020000")
        val HIGH_PRIORITY_FEE: BigInteger = BigInteger("3020000")
        val WITHDRAW_GAS: BigInteger = BigInteger("200000")
        val ENTER_CALL_GAS_LIMITS = listOf(BigInteger("280000"), BigInteger("210000"), BigInteger("140000"))
        val EXIT_CALL_GAS_LIMITS = listOf(BigInteger("280000"), BigInteger("210000"))
    }
}