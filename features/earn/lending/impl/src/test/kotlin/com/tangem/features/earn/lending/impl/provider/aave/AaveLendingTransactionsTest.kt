package com.tangem.features.earn.lending.impl.provider.aave

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory as CallDataFactory
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.fixFee
import com.tangem.domain.yield.supply.models.YieldModuleAddresses
import com.tangem.domain.yield.supply.models.YieldModulePreparation
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase.SimulationPolicy
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.features.earn.lending.impl.provider.LendingProviderState
import com.tangem.features.earn.lending.impl.provider.LendingTxError
import com.tangem.features.earn.lending.impl.provider.LendingTxExpectation
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger
import com.tangem.domain.yield.supply.INCREASE_GAS_LIMIT_FOR_SUPPLY
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.FACTORY
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.LATEST
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.MODULE
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.OLD
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.OWNER
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.coin
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.createTx
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.ethFee
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.wrapped
import com.tangem.test.mock.yield.MockYieldSupply

internal class AaveLendingTransactionsTest {

    private val userWalletId = UserWalletId("011")
    private val userWallet = MockUserWalletFactory.create()
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val weth = currencies.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = WETH,
        symbol = "WETH",
        decimals = 18,
    )
    private val asset = LendingAsset(currency = eth, wrappedToken = weth)
    private val tokenAsset = LendingAsset(currency = weth, wrappedToken = weth)
    private val notNativeCoin = LendingTxError.Unverified("Only a native coin is supported")

    private val repository: YieldSupplyTransactionRepository = mockk()
    private val estimateFee: YieldSupplyEstimateEnterFeeUseCase = mockk()
    private val feeRepository: FeeRepository = mockk()
    private val feeErrorResolver: FeeErrorResolver = mockk()
    private val verifier: AaveTransactionVerifier = mockk()

    private val transactions = AaveLendingTransactions(
        repository = repository,
        estimateFee = estimateFee,
        feeRepository = feeRepository,
        feeErrorResolver = feeErrorResolver,
        verifier = verifier,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, estimateFee, feeRepository, feeErrorResolver, verifier)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Deposit {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN module state WHEN deposit THEN builds preparation and deposit for it`(model: DepositModel) = runTest {
            // Arrange
            arrangeEnter(callData = model.enterCallData)
            arrangePreparation()

            // Act
            val actual = transactions.deposit(userWalletId, asset, DEPOSIT, model.read)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
            if (model.preparation == null) {
                coVerify(exactly = 0) {
                    repository.createNativePreparationTransaction(
                        userWalletId = any(),
                        network = any(),
                        wrappedToken = any(),
                        preparation = any(),
                    )
                }
            } else {
                coVerify(exactly = 1) {
                    repository.createNativePreparationTransaction(
                        userWalletId = userWalletId,
                        network = eth.network,
                        wrappedToken = weth,
                        preparation = model.preparation,
                    )
                }
            }
            val enterCalls = if (model.expected.isRight()) 1 else 0
            coVerify(exactly = enterCalls) {
                repository.createNativeEnterTransaction(
                    userWalletId = userWalletId,
                    network = eth.network,
                    amount = DEPOSIT,
                    wrapIntoUpgrade = model.preparation == null,
                )
            }
        }

        @Test
        fun `GIVEN outdated module that needs init WHEN deposit THEN only the preparation carries the upgrade`() =
            runTest {
                // Arrange
                arrangeEnter(callData = wrapped(enter), wrapIntoUpgrade = true)
                arrangeEnter(callData = enter, wrapIntoUpgrade = false)
                arrangePreparation(callData = wrapped(initToken))

                // Act
                val actual = transactions.deposit(userWalletId, asset, DEPOSIT, read(outdated, needsInitStatus))

                // Assert
                val callData = actual.getOrNull()?.map { tx -> (tx.extras as EthereumTransactionExtras).callData }
                assertThat(callData).isNotNull()
                assertThat(callData!!.first()).isInstanceOf(EthereumYieldSupplyUpgradeToAndCallCallData::class.java)
                assertThat(callData.last()).isSameInstanceAs(enter)
            }

        @ParameterizedTest
        @MethodSource("failureModels")
        fun `GIVEN repository throws WHEN deposit THEN maps the failure`(model: FailureModel) = runTest {
            // Arrange
            coEvery {
                repository.createNativeEnterTransaction(
                    userWalletId = any(),
                    network = any(),
                    amount = any(),
                    wrapIntoUpgrade = any(),
                )
            } throws model.error

            // Act
            val actual = transactions.deposit(userWalletId, asset, DEPOSIT, read(upToDate, readyStatus))

            // Assert
            assertThat(actual).isEqualTo(Either.Left(model.expected))
        }

        @ParameterizedTest
        @MethodSource("preparationFailureModels")
        fun `GIVEN preparation throws WHEN deposit THEN maps the failure`(model: FailureModel) = runTest {
            // Arrange
            arrangeEnter(callData = enter)
            coEvery {
                repository.createNativePreparationTransaction(
                    userWalletId = any(),
                    network = any(),
                    wrappedToken = any(),
                    preparation = any(),
                )
            } throws model.error

            // Act
            val actual = transactions.deposit(userWalletId, asset, DEPOSIT, read(outdated, needsInitStatus))

            // Assert
            assertThat(actual).isEqualTo(Either.Left(model.expected))
        }

        @Test
        fun `GIVEN asset without wrapped token WHEN deposit with preparation THEN read failed`() = runTest {
            // Arrange
            val coinOnly = asset.copy(wrappedToken = null)

            // Act
            val actual = transactions.deposit(userWalletId, coinOnly, DEPOSIT, read(upToDate, needsInitStatus))

            // Assert
            assertThat(actual).isEqualTo(Either.Left(LendingTxError.ReadFailed))
        }

        @Test
        fun `GIVEN state of another provider WHEN deposit THEN read failed`() = runTest {
            // Act
            val actual = transactions.deposit(userWalletId, asset, DEPOSIT, foreignState)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(LendingTxError.ReadFailed))
        }

        @Test
        fun `GIVEN token asset WHEN deposit THEN rejected as not a native coin`() = runTest {
            // Act
            val actual = transactions.deposit(userWalletId, tokenAsset, DEPOSIT, read(upToDate, needsInitStatus))

            // Assert
            assertThat(actual).isEqualTo(Either.Left(notNativeCoin))
        }

        private fun provideTestModels() = listOf(
            DepositModel("ready", read(upToDate, readyStatus), preparation = null),
            DepositModel(
                name = "ready outdated keeps the upgrade",
                read = read(outdated, readyStatus),
                preparation = null,
                enterCallData = wrappedEnter,
            ),
            DepositModel("not deployed", read(YieldModuleVersionStatus.NotDeployed, null), YieldModulePreparation.Deploy),
            DepositModel("needs init", read(upToDate, needsInitStatus), YieldModulePreparation.InitToken),
            DepositModel(
                name = "needs init outdated asks for a bare deposit",
                read = read(outdated, needsInitStatus),
                preparation = YieldModulePreparation.InitToken,
            ),
            DepositModel("needs reactivation", read(upToDate, inactiveStatus), YieldModulePreparation.ReactivateToken),
            DepositModel(
                name = "needs reactivation outdated asks for a bare deposit",
                read = read(outdated, inactiveStatus),
                preparation = YieldModulePreparation.ReactivateToken,
            ),
            DepositModel(
                name = "upgrade unavailable",
                read = read(YieldModuleVersionStatus.UpgradeUnavailable(OLD), readyStatus),
                error = LendingTxError.UpdateRequired,
            ),
            DepositModel(
                name = "read failed",
                read = read(YieldModuleVersionStatus.Indeterminate("rpc"), readyStatus),
                error = LendingTxError.ReadFailed,
            ),
            DepositModel(name = "status unknown", read = read(upToDate, null), error = LendingTxError.ReadFailed),
        )

        private fun failureModels() = failures()

        private fun preparationFailureModels() = failures() + FailureModel(
            name = "version indeterminate",
            error = YieldModuleVersionIndeterminateException(reason = "rpc"),
            expected = LendingTxError.ReadFailed,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Withdraw {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN amount against position WHEN withdraw THEN entire only when it covers the position`(
            model: WithdrawModel,
        ) = runTest {
            // Arrange
            coEvery {
                repository.createNativeWithdrawTransaction(userWalletId = any(), network = any(), amount = any())
            } returns withdrawTx

            // Act
            val actual = transactions.withdraw(userWalletId, asset, model.amount, POSITION, read(upToDate, readyStatus))

            // Assert
            assertThat(actual).isEqualTo(Either.Right(withdrawTx))
            coVerify(exactly = 1) {
                repository.createNativeWithdrawTransaction(
                    userWalletId = userWalletId,
                    network = eth.network,
                    amount = model.expectedAmount,
                )
            }
        }

        @Test
        fun `GIVEN module that cannot be upgraded WHEN withdraw THEN update required`() = runTest {
            // Arrange
            val state = read(YieldModuleVersionStatus.UpgradeUnavailable(OLD), readyStatus)

            // Act
            val actual = transactions.withdraw(userWalletId, asset, POSITION, POSITION, state)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(LendingTxError.UpdateRequired))
            coVerify(exactly = 0) {
                repository.createNativeWithdrawTransaction(userWalletId = any(), network = any(), amount = any())
            }
        }

        @ParameterizedTest
        @MethodSource("failureModels")
        fun `GIVEN repository throws WHEN withdraw THEN maps the failure`(model: FailureModel) = runTest {
            // Arrange
            coEvery {
                repository.createNativeWithdrawTransaction(userWalletId = any(), network = any(), amount = any())
            } throws model.error

            // Act
            val actual = transactions.withdraw(userWalletId, asset, POSITION, POSITION, read(upToDate, readyStatus))

            // Assert
            assertThat(actual).isEqualTo(Either.Left(model.expected))
        }

        @Test
        fun `GIVEN token asset WHEN withdraw THEN rejected as not a native coin`() = runTest {
            // Arrange
            val state = read(upToDate, readyStatus)

            // Act
            val actual = transactions.withdraw(userWalletId, tokenAsset, POSITION, POSITION, state)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(notNativeCoin))
            coVerify(exactly = 0) {
                repository.createNativeWithdrawTransaction(userWalletId = any(), network = any(), amount = any())
            }
        }

        private fun provideTestModels() = listOf(
            WithdrawModel(name = "equal to the position", amount = POSITION, expectedAmount = null),
            WithdrawModel(name = "above the position", amount = POSITION + BigDecimal.ONE, expectedAmount = null),
            WithdrawModel(name = "capped by liquidity", amount = MAX_WITHDRAW, expectedAmount = MAX_WITHDRAW),
        )

        private fun failureModels() = failures()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Estimate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preparation WHEN estimate THEN a revert is fatal and only a deploy has no static fallback`(
            model: EstimatePolicyModel,
        ) = runTest {
            // Arrange
            val txs = listOf(prepTx(model.preparation), enterTx(enter))
            val expected = Either.Right(txs.map { it.copy(fee = ethFee(GAS_PREPARATION)) })
            coEvery {
                estimateFee(userWallet = userWallet, cryptoCurrency = eth, transactionDataList = txs, policy = any())
            } returns expected

            // Act
            val actual = transactions.estimate(userWallet, asset, txs)

            // Assert
            assertThat(actual).isEqualTo(expected)
            coVerify(exactly = 1) {
                estimateFee(
                    userWallet = userWallet,
                    cryptoCurrency = eth,
                    transactionDataList = txs,
                    policy = SimulationPolicy(isRevertFatal = true, isStaticFallbackAllowed = model.isFallbackAllowed),
                )
            }
        }

        private fun provideTestModels() = listOf(
            EstimatePolicyModel(name = "deploy", preparation = deploy, isFallbackAllowed = false),
            EstimatePolicyModel(name = "init", preparation = initToken, isFallbackAllowed = true),
            EstimatePolicyModel(name = "reactivate", preparation = reactivate, isFallbackAllowed = true),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FeeLevels {

        private val txs = listOf(prepTx(), enterTx(enter))
        private val estimated = listOf(
            prepTx().copy(fee = ethFee(GAS_PREPARATION)),
            enterTx(enter).copy(fee = ethFee(GAS_DEPOSIT)),
        )
        private val total = GAS_PREPARATION + GAS_DEPOSIT

        @Test
        fun `GIVEN two transactions WHEN feeLevels THEN every level priced for the summed gas limit`() = runTest {
            // Arrange
            val levels = TransactionFee.Choosable(
                normal = ethFee(GAS_PREPARATION, maxFeePerGas = 20),
                minimum = ethFee(GAS_PREPARATION, maxFeePerGas = 10),
                priority = ethFee(GAS_PREPARATION, maxFeePerGas = 30),
            )
            arrangeEstimate(txs, Either.Right(estimated))
            coEvery {
                feeRepository.calculateFee(userWallet = userWallet, cryptoCurrency = eth, transactionData = txs.first())
            } returns levels

            // Act
            val actual = transactions.feeLevels(userWallet, asset, txs)

            // Assert
            val expected = TransactionFee.Choosable(
                normal = levels.normal.fixFee(eth, total),
                minimum = levels.minimum.fixFee(eth, total),
                priority = levels.priority.fixFee(eth, total),
            )
            assertThat(actual).isEqualTo(Either.Right(expected))
        }

        @Test
        fun `GIVEN single level WHEN feeLevels THEN single level for the summed gas limit`() = runTest {
            // Arrange
            val level = TransactionFee.Single(normal = ethFee(GAS_PREPARATION, maxFeePerGas = 20))
            arrangeEstimate(txs, Either.Right(estimated))
            coEvery {
                feeRepository.calculateFee(userWallet = any(), cryptoCurrency = any(), transactionData = any())
            } returns level

            // Act
            val actual = transactions.feeLevels(userWallet, asset, txs)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(TransactionFee.Single(level.normal.fixFee(eth, total))))
        }

        @Test
        fun `GIVEN single transaction WHEN feeLevels THEN its gas comes from the same fee calculation`() = runTest {
            // Arrange
            val withdrawal = listOf(withdrawTx)
            val levels = TransactionFee.Single(normal = ethFee(GAS_WITHDRAW, maxFeePerGas = 20))
            coEvery {
                feeRepository.calculateFee(userWallet = userWallet, cryptoCurrency = eth, transactionData = withdrawTx)
            } returns levels

            // Act
            val actual = transactions.feeLevels(userWallet, asset, withdrawal)

            // Assert
            val bufferedGas = GAS_WITHDRAW * INCREASE_GAS_LIMIT_FOR_SUPPLY / BigInteger.valueOf(100)
            assertThat(actual).isEqualTo(Either.Right(TransactionFee.Single(levels.normal.fixFee(eth, bufferedGas))))
            coVerify(exactly = 1) {
                feeRepository.calculateFee(userWallet = any(), cryptoCurrency = any(), transactionData = any())
            }
            coVerify(exactly = 0) {
                estimateFee(userWallet = any(), cryptoCurrency = any(), transactionDataList = any(), policy = any())
            }
        }

        @Test
        fun `GIVEN transaction without gas limit WHEN feeLevels THEN fails`() = runTest {
            // Arrange
            arrangeEstimate(txs, Either.Right(listOf(estimated.first(), enterTx(enter))))
            coEvery {
                feeRepository.calculateFee(userWallet = any(), cryptoCurrency = any(), transactionData = any())
            } returns TransactionFee.Single(normal = ethFee(GAS_PREPARATION))

            // Act
            val actual = transactions.feeLevels(userWallet, asset, txs)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(GetFeeError.UnknownError))
        }

        @Test
        fun `GIVEN no transactions WHEN feeLevels THEN fails without a fee calculation`() = runTest {
            // Act
            val actual = transactions.feeLevels(userWallet, asset, emptyList())

            // Assert
            assertThat(actual).isEqualTo(Either.Left(GetFeeError.UnknownError))
            coVerify(exactly = 0) {
                feeRepository.calculateFee(userWallet = any(), cryptoCurrency = any(), transactionData = any())
            }
        }

        @Test
        fun `GIVEN fee calculation throws WHEN feeLevels THEN resolved fee error`() = runTest {
            // Arrange
            val error = IllegalStateException("rpc")
            coEvery {
                feeRepository.calculateFee(userWallet = any(), cryptoCurrency = any(), transactionData = any())
            } throws error
            every { feeErrorResolver.resolve(throwable = error) } returns GetFeeError.DataError(error)

            // Act
            val actual = transactions.feeLevels(userWallet, asset, txs)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(GetFeeError.DataError(error)))
        }

        private fun arrangeEstimate(
            txs: List<TransactionData.Uncompiled>,
            result: Either<GetFeeError, List<TransactionData.Uncompiled>>,
        ) {
            coEvery {
                estimateFee(userWallet = userWallet, cryptoCurrency = eth, transactionDataList = txs, policy = any())
            } returns result
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Verify {

        private val txs = listOf(enterTx(enter))
        private val expectation = LendingTxExpectation.Deposit(owner = OWNER, amount = DEPOSIT)
        private val state = read(upToDate, readyStatus)

        @Test
        fun `GIVEN module addresses WHEN verify THEN verifier checks against them`() = runTest {
            // Arrange
            coEvery {
                repository.getNativeYieldModuleAddresses(userWalletId = userWalletId, network = eth.network)
            } returns YieldModuleAddresses(moduleAddress = MODULE, factoryAddress = FACTORY)
            every {
                verifier.verify(txs = any(), expected = any(), addresses = any(), read = any())
            } returns Either.Left(LendingTxError.Unverified("reason"))

            // Act
            val actual = transactions.verify(userWalletId, asset, txs, expectation, state)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(LendingTxError.Unverified("reason")))
            verify(exactly = 1) {
                verifier.verify(
                    txs = txs,
                    expected = expectation,
                    addresses = AaveVerifiedAddresses(
                        moduleAddress = MODULE,
                        factoryAddress = FACTORY,
                        wrappedTokenAddress = WETH,
                        blockchain = Blockchain.Ethereum,
                    ),
                    read = state,
                )
            }
        }

        @Test
        fun `GIVEN addresses cannot be read WHEN verify THEN read failed`() = runTest {
            // Arrange
            coEvery {
                repository.getNativeYieldModuleAddresses(userWalletId = any(), network = any())
            } throws IllegalStateException("rpc")

            // Act
            val actual = transactions.verify(userWalletId, asset, txs, expectation, state)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(LendingTxError.ReadFailed))
            verify(exactly = 0) { verifier.verify(txs = any(), expected = any(), addresses = any(), read = any()) }
        }

        @Test
        fun `GIVEN state of another provider WHEN verify THEN read failed`() = runTest {
            // Act
            val actual = transactions.verify(userWalletId, asset, txs, expectation, foreignState)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(LendingTxError.ReadFailed))
        }

        @Test
        fun `GIVEN token asset WHEN verify THEN rejected as not a native coin`() = runTest {
            // Act
            val actual = transactions.verify(userWalletId, tokenAsset, txs, expectation, state)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(notNativeCoin))
            verify(exactly = 0) { verifier.verify(txs = any(), expected = any(), addresses = any(), read = any()) }
        }
    }

    private fun arrangeEnter(callData: SmartContractCallData, wrapIntoUpgrade: Boolean? = null) {
        coEvery {
            repository.createNativeEnterTransaction(
                userWalletId = any(),
                network = any(),
                amount = any(),
                wrapIntoUpgrade = wrapIntoUpgrade ?: any(),
            )
        } returns enterTx(callData)
    }

    private fun arrangePreparation(callData: SmartContractCallData = initToken) {
        coEvery {
            repository.createNativePreparationTransaction(
                userWalletId = any(),
                network = any(),
                wrappedToken = any(),
                preparation = any(),
            )
        } returns prepTx(callData)
    }

    data class DepositModel(
        val name: String,
        val read: AaveModuleRead,
        val preparation: YieldModulePreparation? = null,
        val enterCallData: SmartContractCallData = enter,
        val error: LendingTxError? = null,
    ) {
        val expected: Either<LendingTxError, List<TransactionData.Uncompiled>>
            get() = when {
                error != null -> Either.Left(error)
                preparation == null -> Either.Right(listOf(enterTx(enterCallData)))
                else -> Either.Right(listOf(prepTx(), enterTx(enterCallData)))
            }

        override fun toString(): String = name
    }

    data class EstimatePolicyModel(
        val name: String,
        val preparation: SmartContractCallData,
        val isFallbackAllowed: Boolean,
    ) {
        override fun toString(): String = name
    }

    data class WithdrawModel(val name: String, val amount: BigDecimal, val expectedAmount: BigDecimal?) {
        override fun toString(): String = name
    }

    data class FailureModel(val name: String, val error: Throwable, val expected: LendingTxError) {
        override fun toString(): String = name
    }

    private companion object {
        const val WETH = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
        val DEPOSIT = BigDecimal("0.5")
        val POSITION = BigDecimal("2")
        val MAX_WITHDRAW = BigDecimal("1.5")
        val GAS_PREPARATION: BigInteger = BigInteger.valueOf(100_000)
        val GAS_DEPOSIT: BigInteger = BigInteger.valueOf(200_000)
        val GAS_WITHDRAW: BigInteger = BigInteger.valueOf(150_000)

        val enter = CallDataFactory.getEnterWithNativeCallData()
        val initToken = CallDataFactory.getNativeInitTokenCallData(WETH, Blockchain.Ethereum)
        val deploy = CallDataFactory.getNativeDeployCallData(OWNER, WETH, Blockchain.Ethereum)
        val reactivate = CallDataFactory.getNativeReactivateTokenCallData(WETH, Blockchain.Ethereum)
        // The upgrade call data has no equals, so a kept upgrade is compared by the same instance
        val wrappedEnter = wrapped(enter)
        val upToDate = YieldModuleVersionStatus.UpToDate
        val outdated = YieldModuleVersionStatus.UpgradeAvailable(
            currentImplementation = OLD,
            latestImplementation = LATEST,
        )
        val readyStatus = MockYieldSupply.moduleStatus(tokenAddress = WETH)
        val needsInitStatus = MockYieldSupply.moduleStatus(tokenAddress = WETH, isInitialized = false, isActive = false)
        val inactiveStatus = MockYieldSupply.moduleStatus(tokenAddress = WETH, isActive = false)
        val withdrawTx = createTx(callData = CallDataFactory.getExitNativeCallData())
        val foreignState = object : LendingProviderState {
            override val planKey: String = "foreign"
            override val isUpgradeRequired: Boolean = false
        }

        fun read(version: YieldModuleVersionStatus, status: YieldModuleStatus?) =
            AaveModuleRead(status = status, version = version, withdrawLiquidity = null)

        fun enterTx(callData: SmartContractCallData) = createTx(callData = callData, value = DEPOSIT)

        fun prepTx(callData: SmartContractCallData = initToken) = createTx(callData = callData)

        fun failures() = listOf(
            FailureModel(
                name = "upgrade unavailable",
                error = YieldModuleUpgradeUnavailableException(OLD),
                expected = LendingTxError.UpdateRequired,
            ),
            FailureModel(name = "rpc failure", error = IllegalStateException("rpc"), expected = LendingTxError.ReadFailed),
        )
    }
}