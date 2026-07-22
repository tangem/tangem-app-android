package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase.GaslessPayload
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

/**
 * Unit tests for [CreateAndSendGaslessTransactionUseCase.assembleGaslessPayload].
 * Pure function — no coroutines or SDK side-effects.
 */
internal class CreateAndSendGaslessPayloadTest {

    // ─── Common fixtures ─────────────────────────────────────────────────────────

    private val mainTx = GaslessTransactionData.Transaction(
        to = "0xmain",
        value = BigInteger.ZERO,
        gasLimit = BigInteger.valueOf(120_000),
        data = byteArrayOf(0x01, 0x02),
    )

    private val withdrawGasLimit = BigInteger.valueOf(150_000)

    private val feeObj = GaslessTransactionData.Fee(
        feeToken = "0xtoken",
        maxTokenFee = BigInteger.TEN,
        coinPriceInToken = BigInteger.ONE,
        feeTransferGasLimit = BigInteger.valueOf(60_000),
        baseGas = BigInteger.valueOf(21_000),
        feeReceiver = "0xrecv",
    )

    private val nonce = BigInteger.valueOf(42)

    // Minimal SmartContractCallData fake — only `data` is consumed by the SUT.
    private val fakeWithdrawCallData = object : SmartContractCallData {
        override val methodId: String = "0xfakeid"
        override val data: ByteArray = byteArrayOf(0x12, 0x34)
        override fun validate(blockchain: com.tangem.blockchain.common.Blockchain) = true
    }

    private val fakeToken: CryptoCurrency.Token = mockk(relaxed = true)
    private val fakeTokenFee: Fee.Ethereum.TokenCurrency = mockk(relaxed = true)
    private val fakeNativeFee: Fee = mockk(relaxed = true)

    // ─── Case 1: TokenPayWithYieldWithdraw → GaslessPayload.Batch ────────────────

    @Test
    fun `TokenPayWithYieldWithdraw plan returns Batch with correct structure`() {
        val plan = GaslessFeePlan.TokenPayWithYieldWithdraw(
            feeToken = fakeToken,
            fee = fakeTokenFee,
            withdrawAmount = BigInteger.valueOf(7_000_001),
            withdrawCallData = fakeWithdrawCallData,
            yieldModuleAddress = "0xmodule",
        )

        val result = CreateAndSendGaslessTransactionUseCase.assembleGaslessPayload(
            mainTx = mainTx,
            feeObj = feeObj,
            nonce = nonce,
            plan = plan,
            withdrawGasLimit = withdrawGasLimit,
        )

        assertThat(result).isInstanceOf(GaslessPayload.Batch::class.java)
        val batch = (result as GaslessPayload.Batch).data

        // transactions list has exactly 2 entries
        assertThat(batch.transactions).hasSize(2)

        // index 0 is the unchanged main transaction
        assertThat(batch.transactions[0]).isEqualTo(mainTx)

        // index 1 is the yield-withdraw transaction
        val withdrawTx = batch.transactions[1]
        assertThat(withdrawTx.to).isEqualTo(plan.yieldModuleAddress)
        assertThat(withdrawTx.value).isEqualTo(BigInteger.ZERO)
        assertThat(withdrawTx.gasLimit).isEqualTo(withdrawGasLimit)
        assertThat(withdrawTx.data).isEqualTo(fakeWithdrawCallData.data)

        // fee and nonce are carried through
        assertThat(batch.fee).isEqualTo(feeObj)
        assertThat(batch.nonce).isEqualTo(nonce)
    }

    // ─── Case 2: TokenPay → GaslessPayload.Single ────────────────────────────────

    @Test
    fun `TokenPay plan returns Single wrapping mainTx feeObj and nonce`() {
        val plan = GaslessFeePlan.TokenPay(feeToken = fakeToken, fee = fakeTokenFee)

        val result = CreateAndSendGaslessTransactionUseCase.assembleGaslessPayload(
            mainTx = mainTx,
            feeObj = feeObj,
            nonce = nonce,
            plan = plan,
            withdrawGasLimit = null,
        )

        assertThat(result).isInstanceOf(GaslessPayload.Single::class.java)
        val single = (result as GaslessPayload.Single).data
        assertThat(single.transaction).isEqualTo(mainTx)
        assertThat(single.fee).isEqualTo(feeObj)
        assertThat(single.nonce).isEqualTo(nonce)
    }

    // ─── Case 3: null plan → GaslessPayload.Single (same as TokenPay) ───────────

    @Test
    fun `null plan returns Single wrapping mainTx feeObj and nonce`() {
        val result = CreateAndSendGaslessTransactionUseCase.assembleGaslessPayload(
            mainTx = mainTx,
            feeObj = feeObj,
            nonce = nonce,
            plan = null,
            withdrawGasLimit = null,
        )

        assertThat(result).isInstanceOf(GaslessPayload.Single::class.java)
        val single = (result as GaslessPayload.Single).data
        assertThat(single.transaction).isEqualTo(mainTx)
        assertThat(single.fee).isEqualTo(feeObj)
        assertThat(single.nonce).isEqualTo(nonce)
    }

    // ─── Case 4: NativePay → throws IllegalStateException ───────────────────────

    @Test
    fun `NativePay plan throws IllegalStateException`() {
        val plan = GaslessFeePlan.NativePay(fee = fakeNativeFee)

        assertThrows<IllegalStateException> {
            CreateAndSendGaslessTransactionUseCase.assembleGaslessPayload(
                mainTx = mainTx,
                feeObj = feeObj,
                nonce = nonce,
                plan = plan,
                withdrawGasLimit = null,
            )
        }
    }

    // ─── Case 5: yield-withdraw plan without a withdraw gas limit → throws ────────

    @Test
    fun `TokenPayWithYieldWithdraw plan without withdrawGasLimit throws IllegalStateException`() {
        val plan = GaslessFeePlan.TokenPayWithYieldWithdraw(
            feeToken = fakeToken,
            fee = fakeTokenFee,
            withdrawAmount = BigInteger.valueOf(7_000_001),
            withdrawCallData = fakeWithdrawCallData,
            yieldModuleAddress = "0xmodule",
        )

        assertThrows<IllegalStateException> {
            CreateAndSendGaslessTransactionUseCase.assembleGaslessPayload(
                mainTx = mainTx,
                feeObj = feeObj,
                nonce = nonce,
                plan = plan,
                withdrawGasLimit = null,
            )
        }
    }
}