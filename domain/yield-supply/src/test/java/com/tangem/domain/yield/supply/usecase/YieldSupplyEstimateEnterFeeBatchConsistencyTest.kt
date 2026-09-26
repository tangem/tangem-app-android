package com.tangem.domain.yield.supply.usecase

import arrow.core.right
import com.domain.blockaid.models.transaction.GasEstimationResult
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.domain.blockaid.BlockAidGasEstimate
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class YieldSupplyEstimateEnterFeeBatchConsistencyTest {

    private val feeRepository: FeeRepository = mockk()
    private val blockAidGasEstimate: BlockAidGasEstimate = mockk()
    private val feeErrorResolver: FeeErrorResolver = mockk(relaxed = true)
    private val useCase = YieldSupplyEstimateEnterFeeUseCase(feeRepository, feeErrorResolver, blockAidGasEstimate)

    private val userWallet: UserWallet = mockk(relaxed = true)
    private val token: CryptoCurrency.Token = mockk(relaxed = true) {
        every { decimals } returns 18
    }

    private val baseFee = Fee.Ethereum.EIP1559(
        amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
        gasLimit = BigInteger.ZERO,
        maxFeePerGas = BigInteger.valueOf(1_000_000_000L),
        priorityFee = BigInteger.ONE,
    )

    private val deployTx = plainTx()
    private val approveTx = plainTx()
    private val enterTx = enterTx()
    private val batch = listOf(deployTx, approveTx, enterTx)

    @Test
    fun `GIVEN estimate list matches batch WHEN invoke THEN every tx gets its BlockAid gas`() = runTest {
        coEvery { blockAidGasEstimate.getGasEstimation(token, batch) } returns
            GasEstimationResult(listOf(300_000, 60_000, 200_000).map(Int::toBigInteger)).right()
        coEvery { feeRepository.getEthereumFeeWithoutGas(any(), token) } returns baseFee

        val result = useCase(userWallet, token, batch).getOrNull()!!

        assertThat(result).hasSize(3)
        assertThat(result.map { (it.fee as Fee.Ethereum).gasLimit })
            .containsExactly(420_000.toBigInteger(), 84_000.toBigInteger(), 280_000.toBigInteger())
            .inOrder()
        coVerify(exactly = 0) { feeRepository.calculateFee(any(), any(), any()) }
    }

    @Test
    fun `GIVEN estimate list shorter than batch WHEN invoke THEN static fallback keeps every transaction`() = runTest {
        coEvery { blockAidGasEstimate.getGasEstimation(token, batch) } returns
            GasEstimationResult(listOf(300_000, 60_000).map(Int::toBigInteger)).right()
        coEvery { feeRepository.calculateFee(any(), token, any()) } returns TransactionFee.Single(baseFee)

        val result = useCase(userWallet, token, batch).getOrNull()!!

        // the previous zip() would have returned 2 transactions and silently dropped `enter`
        assertThat(result).hasSize(3)
        val enterCount = result.count {
            (it.extras as? EthereumTransactionExtras)?.callData is EthereumYieldSupplyEnterCallData
        }
        assertThat(enterCount).isEqualTo(1)
        coVerify(exactly = 0) { feeRepository.getEthereumFeeWithoutGas(any(), any()) }
    }

    @Test
    fun `GIVEN implausible estimate WHEN invoke THEN static fallback is used`() = runTest {
        coEvery { blockAidGasEstimate.getGasEstimation(token, batch) } returns
            GasEstimationResult(
                listOf(300_000.toBigInteger(), 60_000.toBigInteger(), BigInteger.valueOf(50_000_000_000L)),
            ).right()
        coEvery { feeRepository.calculateFee(any(), token, any()) } returns TransactionFee.Single(baseFee)

        val result = useCase(userWallet, token, batch).getOrNull()!!

        assertThat(result).hasSize(3)
        coVerify(exactly = 0) { feeRepository.getEthereumFeeWithoutGas(any(), any()) }
    }

    private fun plainTx(): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
        fee = null,
        sourceAddress = "0xSource",
        destinationAddress = "0xDestination",
        extras = EthereumTransactionExtras(callData = mockk(relaxed = true)),
    )

    private fun enterTx(): TransactionData.Uncompiled = TransactionData.Uncompiled(
        amount = Amount(value = BigDecimal.ZERO, blockchain = Blockchain.Ethereum),
        fee = null,
        sourceAddress = "0xSource",
        destinationAddress = "0xModule",
        extras = EthereumTransactionExtras(callData = EthereumYieldSupplyEnterCallData(tokenContractAddress = "0xToken")),
    )
}
