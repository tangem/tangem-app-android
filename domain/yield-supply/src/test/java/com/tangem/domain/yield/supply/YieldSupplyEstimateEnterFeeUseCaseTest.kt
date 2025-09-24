package com.tangem.domain.yield.supply

import arrow.core.Either
import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.SmartContractCallDataProviderFactory
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
class YieldSupplyEstimateEnterFeeUseCaseTest {
    private val feeRepository: FeeRepository = mockk()
    private val feeErrorResolver: FeeErrorResolver = mockk()
    private val useCase = YieldSupplyEstimateEnterFeeUseCase(feeRepository, feeErrorResolver)

    private val userWallet: UserWallet = mockk()
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

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(
                getDeployTx(),
                getEnterTx(),
            ),
        )
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value

        val deployFee = txs.first().fee as Fee.Ethereum.Legacy
        val enterFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }

    @Test
    fun `test not enter transaction uses gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(
                getDeployTx(),
                getApproveTx(),
            ),
        )
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value

        val deployFee = txs.first().fee as Fee.Ethereum.Legacy
        val approveFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
    }

    @Test
    fun `test enter transaction with wrong order uses gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(
                getEnterTx(),
                getDeployTx(),
            ),
        )
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value

        val deployFee = txs.first().fee as Fee.Ethereum.Legacy
        val enterFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }

    @Test
    fun `test enter transaction uses constant gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(
                getDeployTx(),
                getEnterTx(),
            ),
        )
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value

        val deployFee = txs.first().fee as Fee.Ethereum.EIP1559
        val enterFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }

    @Test
    fun `test not enter transaction uses gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(
                getDeployTx(),
                getApproveTx(),
            ),
        )
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value

        val deployFee = txs.first().fee as Fee.Ethereum.EIP1559
        val approveFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(approveFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
    }

    @Test
    fun `test enter transaction with wrong order uses gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = listOf(
                getEnterTx(),
                getDeployTx(),
            ),
        )
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value

        val deployFee = txs.first().fee as Fee.Ethereum.EIP1559
        val enterFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(deployFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(enterFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
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