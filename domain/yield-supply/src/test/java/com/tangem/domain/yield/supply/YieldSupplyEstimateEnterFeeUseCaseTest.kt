package com.tangem.domain.yield.supply

import arrow.core.Either
import com.google.common.truth.Truth
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency
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

    private fun ethLegacyFee(
        gasPrice: BigInteger = BigInteger.valueOf(100_000_000_000L),
        gasLimit: BigInteger = BigInteger.valueOf(21_000),
    ) = Fee.Ethereum.Legacy(
        gasPrice = gasPrice,
        gasLimit = gasLimit,
        amount = BigDecimal.ONE.convertToSdkAmount(cryptoCurrency),
    )

    private fun ethEip1559Fee(
        maxFeePerGas: BigInteger = BigInteger.valueOf(100_000_000_000L),
        gasLimit: BigInteger = BigInteger.valueOf(21_000),
    ) = Fee.Ethereum.EIP1559(
        maxFeePerGas = maxFeePerGas,
        priorityFee = BigInteger.ONE,
        gasLimit = gasLimit,
        amount = BigDecimal.ONE.convertToSdkAmount(cryptoCurrency),
    )

    private fun uncompiled(fee: Fee) = TransactionData.Uncompiled(
        fee = fee,
        amount = BigDecimal.ONE.convertToSdkAmount(cryptoCurrency),
        contractAddress = null,
        sourceAddress = "0x1234567890123456789012345678901234567890",
        destinationAddress = "0x1234567890123456789012345678901234567890",
    )

    @Test
    fun `test 1 transaction uses constant gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val tx = uncompiled(ethLegacyFee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

        val result = useCase(userWallet, cryptoCurrency, listOf(tx))
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value
        Truth.assertThat(txs.size).isEqualTo(1)

        val lastFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(lastFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }

    @Test
    fun `test 2 transactions, only last uses constant gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val tx = uncompiled(ethLegacyFee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(userWallet, cryptoCurrency, listOf(tx, tx))
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value
        Truth.assertThat(txs.size).isEqualTo(2)

        val firstFee = txs.first().fee as Fee.Ethereum.Legacy
        val lastFee = txs.last().fee as Fee.Ethereum.Legacy
        Truth.assertThat(firstFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(lastFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }

    @Test
    fun `test 3 transactions, only last uses constant gas limit Legacy`() = runTest {
        val fee = TransactionFee.Single(ethLegacyFee())
        val tx = uncompiled(ethLegacyFee())
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee, fee)

        val result = useCase(userWallet, cryptoCurrency, listOf(tx, tx, tx))
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value
        Truth.assertThat(txs.size).isEqualTo(3)

        val firstFee = txs[0].fee as Fee.Ethereum.Legacy
        val secondFee = txs[1].fee as Fee.Ethereum.Legacy
        val lastFee = txs[2].fee as Fee.Ethereum.Legacy
        Truth.assertThat(firstFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(secondFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(lastFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }

    @Test
    fun `test 1 transaction uses constant gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val tx = uncompiled(ethEip1559Fee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returns fee

        val result = useCase(userWallet, cryptoCurrency, listOf(tx))
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value
        Truth.assertThat(txs.size).isEqualTo(1)

        val lastFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(lastFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }

    @Test
    fun `test 2 transactions, only last uses constant gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val tx = uncompiled(ethEip1559Fee())

        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee)

        val result = useCase(userWallet, cryptoCurrency, listOf(tx, tx))
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value
        Truth.assertThat(txs.size).isEqualTo(2)

        val firstFee = txs.first().fee as Fee.Ethereum.EIP1559
        val lastFee = txs.last().fee as Fee.Ethereum.EIP1559
        Truth.assertThat(firstFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(lastFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }

    @Test
    fun `test 3 transactions, only last uses constant gas limit Eip1559`() = runTest {
        val fee = TransactionFee.Single(ethEip1559Fee())
        val tx = uncompiled(ethEip1559Fee())
        coEvery { feeRepository.calculateFee(any(), any(), any()) } returnsMany listOf(fee, fee, fee)

        val result = useCase(userWallet, cryptoCurrency, listOf(tx, tx, tx))
        Truth.assertThat(result.isRight()).isTrue()

        val txs = (result as Either.Right).value
        Truth.assertThat(txs.size).isEqualTo(3)

        val firstFee = txs[0].fee as Fee.Ethereum.EIP1559
        val secondFee = txs[1].fee as Fee.Ethereum.EIP1559
        val lastFee = txs[2].fee as Fee.Ethereum.EIP1559
        Truth.assertThat(firstFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(secondFee.gasLimit).isEqualTo(BigInteger.valueOf(21_000))
        Truth.assertThat(lastFee.gasLimit).isEqualTo(BigInteger.valueOf(350_000))
    }
}