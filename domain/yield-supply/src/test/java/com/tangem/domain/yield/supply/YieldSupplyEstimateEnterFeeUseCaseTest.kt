package com.tangem.domain.yield.supply

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.transaction.GasEstimationResult
import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractCallDataProviderFactory
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.blockaid.BlockAidGasEstimate
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase.SimulationPolicy
import com.tangem.test.core.ProvideTestModels
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class YieldSupplyEstimateEnterFeeUseCaseTest {
    private val feeRepository: FeeRepository = mockk()
    private val feeErrorResolver: FeeErrorResolver = mockk()
    private val blockAidGasEstimate: BlockAidGasEstimate = mockk()
    private val useCase = YieldSupplyEstimateEnterFeeUseCase(feeRepository, feeErrorResolver, blockAidGasEstimate)

    private val userWallet: UserWallet = mockk {
        every { walletId } returns mockk()
    }
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true) {
        every { decimals } returns 18
    }
    private val cryptoCurrencyStatus = mockk<CryptoCurrencyStatus>(relaxed = true) {
        every { currency } returns cryptoCurrency
        every { value.yieldSupplyStatus } returns null
    }

    private fun ethLegacyFee(
        gasPrice: BigInteger = BigInteger.valueOf(100_000_000_000L),
        gasLimit: BigInteger = BigInteger.valueOf(21_000),
    ) = Fee.Ethereum.Legacy(
        gasPrice = gasPrice,
        gasLimit = gasLimit,
        amount = BigDecimal.ONE.convertToSdkAmount(cryptoCurrencyStatus),
    )

    private fun ethEip1559Fee(
        maxFeePerGas: BigInteger = BigInteger.valueOf(100_000_000_000L),
        gasLimit: BigInteger = BigInteger.valueOf(21_000),
    ) = Fee.Ethereum.EIP1559(
        maxFeePerGas = maxFeePerGas,
        priorityFee = BigInteger.ONE,
        gasLimit = gasLimit,
        amount = BigDecimal.ONE.convertToSdkAmount(cryptoCurrencyStatus),
    )

    private fun uncompiled(fee: Fee, extras: TransactionExtras) = TransactionData.Uncompiled(
        fee = fee,
        amount = BigDecimal.ONE.convertToSdkAmount(cryptoCurrencyStatus),
        contractAddress = null,
        sourceAddress = "0x1234567890123456789012345678901234567890",
        destinationAddress = "0x1234567890123456789012345678901234567890",
        extras = extras,
    )

    @Test
    fun `test enter transaction uses constant gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val deployTx = getDeployTx()
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Unavailable.right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(deployTx, enterTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            blockAidGasEstimate.getGasEstimation(any(), any())
            feeRepository.calculateFee(any(), any(), deployTx)
        }
        coVerify(inverse = true) {
            feeRepository.calculateFee(any(), any(), enterTx)
        }

        val txs = (result as Either.Right).value
        val deployFee = txs.first().fee as Fee.Ethereum.Legacy
        val enterFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(700_000))
        Truth.assertThat(enterFee.amount.value).isEqualToIgnoringScale(BigDecimal("0.07"))
    }

    @Test
    fun `GIVEN upgrade-wrapped enter WHEN invoke without estimation THEN it is treated as enter and gets constant gas limit`() =
        runTest {
            // Arrange
            val fee = TransactionFee.Single(ethLegacyFee())
            val deployTx = getDeployTx()
            val wrappedEnterTx = getEnterTx().let { tx ->
                val enterCallData = (tx.extras as EthereumTransactionExtras).callData!!
                tx.copy(
                    extras = EthereumTransactionExtras(
                        EthereumYieldSupplyUpgradeToAndCallCallData(
                            newImplementation = "0x5555555555555555555555555555555555555555",
                            innerCallData = enterCallData,
                        ),
                    ),
                )
            }
            coEvery {
                blockAidGasEstimate.getGasEstimation(any(), any())
            } returns GasEstimationResult.Unavailable.right()
            coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

            // Act
            val result = useCase(
                userWallet = userWallet,
                cryptoCurrency = cryptoCurrency,
                transactionDataList = listOf(deployTx, wrappedEnterTx),
            )

            // Assert
            val txs = (result as Either.Right).value
            Truth.assertThat(txs.map { (it.fee as Fee.Ethereum.Legacy).gasLimit })
                .containsExactly(BigInteger.valueOf(29_400), BigInteger.valueOf(700_000))
                .inOrder()
            coVerify(exactly = 0) { feeRepository.calculateFee(any(), any(), wrappedEnterTx) }
        }

    @Test
    fun `GIVEN blockaid fails AND list of preparation and native enter WHEN invoke THEN native enter gets constant gas limit increased`() =
        runTest {
            // Arrange
            val fee = TransactionFee.Single(ethLegacyFee())
            val preparationTx = getDeployTx()
            val nativeEnterTx = uncompiled(
                fee = ethLegacyFee(),
                extras = EthereumTransactionExtras(YieldSupplyContractCallDataProviderFactory.getEnterWithNativeCallData()),
            )
            coEvery {
                blockAidGasEstimate.getGasEstimation(any(), any())
            } returns GasEstimationResult.Unavailable.right()
            coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

            // Act
            val result = useCase(
                userWallet = userWallet,
                cryptoCurrency = cryptoCurrency,
                transactionDataList = listOf(preparationTx, nativeEnterTx),
            )

            // Assert
            val txs = (result as Either.Right).value
            Truth.assertThat(txs.map { (it.fee as Fee.Ethereum.Legacy).gasLimit })
                .containsExactly(BigInteger.valueOf(29_400), BigInteger.valueOf(700_000))
                .inOrder()
            coVerify(exactly = 0) { feeRepository.calculateFee(any(), any(), nativeEnterTx) }
        }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SimulationOutcome {

        private val fee = TransactionFee.Single(ethLegacyFee())
        private val deployTx = getDeployTx()
        private val enterTx = getEnterTx()

        @BeforeEach
        fun resetMocks() {
            clearMocks(feeRepository, feeErrorResolver, blockAidGasEstimate)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN simulation outcome and policy WHEN invoke THEN fails or falls back to the static estimate`(
            model: SimulationOutcomeModel,
        ) = runTest {
            // Arrange
            coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns model.simulation
            coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee
            val staticEstimate = listOf(
                deployTx.copy(fee = fee.normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY)),
                enterTx.copy(
                    fee = fee.normal.fixFee(cryptoCurrency, 500_000.toBigInteger())
                        .increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                ),
            )
            val expected: Either<GetFeeError, List<TransactionData.Uncompiled>> =
                model.expectedError?.left() ?: staticEstimate.right()

            // Act
            val actual = useCase(
                userWallet = userWallet,
                cryptoCurrency = cryptoCurrency,
                transactionDataList = listOf(deployTx, enterTx),
                policy = model.policy,
            )

            // Assert
            Truth.assertThat(actual).isEqualTo(expected)
            coVerify(exactly = if (model.expectedError == null) 1 else 0) {
                feeRepository.calculateFee(any(), any(), any())
            }
            verify(exactly = 0) { feeErrorResolver.resolve(any()) }
        }

        private fun provideTestModels() = listOf(
            SimulationOutcomeModel(
                name = "reverted, default policy",
                simulation = GasEstimationResult.Reverted.right(),
                policy = SimulationPolicy(),
                expectedError = null,
            ),
            SimulationOutcomeModel(
                name = "reverted, revert not fatal, no static fallback",
                simulation = GasEstimationResult.Reverted.right(),
                policy = SimulationPolicy(isRevertFatal = false, isStaticFallbackAllowed = false),
                expectedError = null,
            ),
            SimulationOutcomeModel(
                name = "reverted, revert fatal",
                simulation = GasEstimationResult.Reverted.right(),
                policy = SimulationPolicy(isRevertFatal = true, isStaticFallbackAllowed = true),
                expectedError = GetFeeError.SimulationError.Reverted,
            ),
            SimulationOutcomeModel(
                name = "reverted, revert fatal, no static fallback",
                simulation = GasEstimationResult.Reverted.right(),
                policy = SimulationPolicy(isRevertFatal = true, isStaticFallbackAllowed = false),
                expectedError = GetFeeError.SimulationError.Reverted,
            ),
            SimulationOutcomeModel(
                name = "unavailable, default policy",
                simulation = GasEstimationResult.Unavailable.right(),
                policy = SimulationPolicy(),
                expectedError = null,
            ),
            SimulationOutcomeModel(
                name = "unavailable, revert fatal",
                simulation = GasEstimationResult.Unavailable.right(),
                policy = SimulationPolicy(isRevertFatal = true, isStaticFallbackAllowed = true),
                expectedError = null,
            ),
            SimulationOutcomeModel(
                name = "unavailable, no static fallback",
                simulation = GasEstimationResult.Unavailable.right(),
                policy = SimulationPolicy(isRevertFatal = false, isStaticFallbackAllowed = false),
                expectedError = GetFeeError.SimulationError.Unavailable,
            ),
            SimulationOutcomeModel(
                name = "blockaid failed, no static fallback",
                simulation = IllegalStateException("blockaid").left(),
                policy = SimulationPolicy(isRevertFatal = true, isStaticFallbackAllowed = false),
                expectedError = GetFeeError.SimulationError.Unavailable,
            ),
        )
    }

    data class SimulationOutcomeModel(
        val name: String,
        val simulation: Either<Throwable, GasEstimationResult>,
        val policy: SimulationPolicy,
        val expectedError: GetFeeError?,
    ) {
        override fun toString(): String = name
    }

    @Test
    fun `test not enter transaction uses gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val deployTx = getDeployTx()
        val approveTx = getApproveTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Unavailable.right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(deployTx, approveTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            blockAidGasEstimate.getGasEstimation(any(), any())
            feeRepository.calculateFee(any(), any(), deployTx)
            feeRepository.calculateFee(any(), any(), approveTx)
        }

        val txs = (result as Either.Right).value
        val deployFee = txs.first().fee as Fee.Ethereum.Legacy
        val approveFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
    }

    @Test
    fun `test enter transaction with wrong order uses gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val enterTx = getEnterTx()
        val deployTx = getDeployTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Unavailable.right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(enterTx, deployTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            blockAidGasEstimate.getGasEstimation(any(), any())
            feeRepository.calculateFee(any(), any(), deployTx)
        }
        coVerify(inverse = true) {
            feeRepository.calculateFee(any(), any(), enterTx)
        }

        val txs = (result as Either.Right).value
        val deployFee = txs.first().fee as Fee.Ethereum.Legacy
        val enterFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(700_000))
    }

    @Test
    fun `test enter transaction uses constant gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val deployTx = getDeployTx()
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Unavailable.right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(deployTx, enterTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            blockAidGasEstimate.getGasEstimation(any(), any())
            feeRepository.calculateFee(any(), any(), deployTx)
        }
        coVerify(inverse = true) {
            feeRepository.calculateFee(any(), any(), enterTx)
        }

        val txs = (result as Either.Right).value
        val deployFee = txs.first().fee as Fee.Ethereum.EIP1559
        val enterFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(700_000))
        Truth.assertThat(enterFee.amount.value).isEqualToIgnoringScale(BigDecimal("0.07"))
    }

    @Test
    fun `test not enter transaction uses gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val deployTx = getDeployTx()
        val approveTx = getApproveTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Unavailable.right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(deployTx, approveTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            blockAidGasEstimate.getGasEstimation(any(), any())
            feeRepository.calculateFee(any(), any(), deployTx)
            feeRepository.calculateFee(any(), any(), approveTx)
        }

        val txs = (result as Either.Right).value
        val deployFee = txs.first().fee as Fee.Ethereum.EIP1559
        val approveFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
    }

    @Test
    fun `test enter transaction with wrong order uses gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val enterTx = getEnterTx()
        val deployTx = getDeployTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Unavailable.right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(enterTx, deployTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            blockAidGasEstimate.getGasEstimation(any(), any())
            feeRepository.calculateFee(any(), any(), deployTx)
        }
        coVerify(inverse = true) {
            feeRepository.calculateFee(any(), any(), enterTx)
        }

        val txs = (result as Either.Right).value
        val deployFee = txs.first().fee as Fee.Ethereum.EIP1559
        val enterFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(700_000))
    }

    @Test
    fun `test single transaction with fee Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Unavailable.right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(enterTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            feeRepository.calculateFee(any(), any(), enterTx)
        }
        coVerify(inverse = true) {
            blockAidGasEstimate.getGasEstimation(any(), any())
        }

        val txs = (result as Either.Right).value
        val enterFee = txs.first().fee as Fee.Ethereum.Legacy
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(29_400))
    }

    @Test
    fun `test enter transaction uses block aid Eip1559`() = runTest {
        val fee = ethEip1559Fee()
        val deployTx = getDeployTx()
        val approveTx = getApproveTx()
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Estimated(
            gasLimits = listOf(1_000.toBigInteger(), 2_000.toBigInteger(), 3_000.toBigInteger()),
        ).right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns mockk()
        coEvery { feeRepository.getEthereumFeeWithoutGas(any(), any()) } returns fee

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(deployTx, approveTx, enterTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            blockAidGasEstimate.getGasEstimation(any(), any())
            feeRepository.getEthereumFeeWithoutGas(any(), any())
        }
        coVerify(inverse = true) {
            feeRepository.calculateFee(any(), any(), deployTx)
            feeRepository.calculateFee(any(), any(), approveTx)
            feeRepository.calculateFee(any(), any(), enterTx)
        }

        val txs = (result as Either.Right).value
        val deployFee = txs.first().fee as Fee.Ethereum.EIP1559
        val approveFee = txs[1].fee as Fee.Ethereum.EIP1559
        val enterFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(1_400))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(2_800))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(4_200))
    }

    @Test
    fun `test enter transaction uses block aid Legacy`() = runTest {
        val fee = ethLegacyFee()
        val deployTx = getDeployTx()
        val approveTx = getApproveTx()
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult.Estimated(
            gasLimits = listOf(1_000.toBigInteger(), 2_000.toBigInteger(), 3_000.toBigInteger()),
        ).right()
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns mockk()
        coEvery { feeRepository.getEthereumFeeWithoutGas(any(), any()) } returns fee

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(deployTx, approveTx, enterTx),
        )
        Truth.assertThat(result.isRight()).isTrue()

        coVerify(ordering = Ordering.ORDERED) {
            blockAidGasEstimate.getGasEstimation(any(), any())
            feeRepository.getEthereumFeeWithoutGas(any(), any())
        }
        coVerify(inverse = true) {
            feeRepository.calculateFee(any(), any(), deployTx)
            feeRepository.calculateFee(any(), any(), approveTx)
            feeRepository.calculateFee(any(), any(), enterTx)
        }

        val txs = (result as Either.Right).value
        val deployFee = txs.first().fee as Fee.Ethereum.Legacy
        val approveFee = txs[1].fee as Fee.Ethereum.Legacy
        val enterFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(1_400))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(2_800))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(4_200))
    }

    private fun getDeployTx() = uncompiled(
        fee = ethLegacyFee(),
        extras = EthereumTransactionExtras(
            YieldSupplyContractCallDataProviderFactory.getDeployCallData(
                walletAddress = "0x1234567890123456789012345678901234567890",
                tokenContractAddress = "0x1234567890123456789012345678901234567890",
                maxNetworkFee = BigDecimal.ONE.convertToSdkAmount(cryptoCurrencyStatus),
            ),
        ),
    )

    private fun getApproveTx() = uncompiled(
        fee = ethLegacyFee(),
        extras = EthereumTransactionExtras(
            SmartContractCallDataProviderFactory.getApprovalCallData(
                spenderAddress = "0x1234567890123456789012345678901234567890",
                amount = null,
                blockchain = Blockchain.EthereumTestnet,
            ),
        ),
    )

    private fun getEnterTx() = uncompiled(
        fee = ethLegacyFee(),
        extras = EthereumTransactionExtras(
            YieldSupplyContractCallDataProviderFactory.getEnterCallData(
                tokenContractAddress = "0x1234567890123456789012345678901234567890",
            ),
        ),
    )
}