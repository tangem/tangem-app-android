package com.tangem.domain.yield.supply

import arrow.core.Either
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
import com.tangem.domain.blockaid.BlockAidGasEstimate
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
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

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(emptyList()).right()
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
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(500_000))
    }

    @Test
    fun `test not enter transaction uses gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val deployTx = getDeployTx()
        val approveTx = getApproveTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(emptyList()).right()
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
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
    }

    @Test
    fun `test enter transaction with wrong order uses gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val enterTx = getEnterTx()
        val deployTx = getDeployTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(emptyList()).right()
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
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(500_000))
    }

    @Test
    fun `test enter transaction uses constant gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val deployTx = getDeployTx()
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(emptyList()).right()
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
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(500_000))
    }

    @Test
    fun `test not enter transaction uses gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val deployTx = getDeployTx()
        val approveTx = getApproveTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(emptyList()).right()
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
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
    }

    @Test
    fun `test enter transaction with wrong order uses gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val enterTx = getEnterTx()
        val deployTx = getDeployTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(emptyList()).right()
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
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(500_000))
    }

    @Test
    fun `test single transaction with fee Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(emptyList()).right()
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
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
    }

    @Test
    fun `test enter transaction uses block aid Eip1559`() = runTest {
        val fee = ethEip1559Fee()
        val deployTx = getDeployTx()
        val approveTx = getApproveTx()
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(
            estimatedGasList = listOf(1_000.toBigInteger(), 2_000.toBigInteger(), 3_000.toBigInteger()),
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
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(1_200))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(2_400))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(3_600))
    }

    @Test
    fun `test enter transaction uses block aid Legacy`() = runTest {
        val fee = ethLegacyFee()
        val deployTx = getDeployTx()
        val approveTx = getApproveTx()
        val enterTx = getEnterTx()

        coEvery { blockAidGasEstimate.getGasEstimation(any(), any()) } returns GasEstimationResult(
            estimatedGasList = listOf(1_000.toBigInteger(), 2_000.toBigInteger(), 3_000.toBigInteger()),
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
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(1_200))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(2_400))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(3_600))
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