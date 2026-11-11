package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyUpgradeToAndCallCallData
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.GaslessApproval
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.transaction.models.GaslessTransactionData
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendEvmGaslessTransactionUseCase.GaslessPayload
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

/**
 * Unit tests for [CreateAndSendEvmGaslessTransactionUseCase.assembleGaslessPayload].
 * Pure function — no coroutines or SDK side-effects.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateAndSendEvmGaslessPayloadTest {

    private val mainTx = GaslessTransactionData.Transaction(
        to = "0xmain",
        value = BigInteger.ZERO,
        gasLimit = BigInteger.valueOf(120_000),
        data = byteArrayOf(0x01, 0x02),
    )

    private val deployTx = GaslessTransactionData.Transaction(
        to = "0xfactory",
        value = BigInteger.ZERO,
        gasLimit = BigInteger.valueOf(400_000),
        data = byteArrayOf(0x03),
    )

    private val approveTx = GaslessTransactionData.Transaction(
        to = "0xtoken",
        value = BigInteger.ZERO,
        gasLimit = BigInteger.valueOf(60_000),
        data = byteArrayOf(0x04),
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

    private val withdrawTx = GaslessTransactionData.Transaction(
        to = "0xmodule",
        value = BigInteger.ZERO,
        gasLimit = withdrawGasLimit,
        data = fakeWithdrawCallData.data,
    )

    private val fakeToken: CryptoCurrency.Token = mockk(relaxed = true)
    private val fakeTokenFee: Fee.Ethereum.TokenCurrency = mockk(relaxed = true)
    private val fakeNativeFee: Fee = mockk(relaxed = true)

    @Test
    fun `GIVEN one call and TokenPay WHEN assembleGaslessPayload THEN returns Single`() {
        // Arrange
        val plan = GaslessFeePlan.TokenPay(feeToken = fakeToken, fee = fakeTokenFee)

        // Act
        val result = assemble(calls = listOf(mainTx), plan = plan)

        // Assert
        assertThat(result).isEqualTo(
            GaslessPayload.Single(GaslessTransactionData(transaction = mainTx, fee = feeObj, nonce = nonce)),
        )
    }

    @Test
    fun `GIVEN one call and null plan WHEN assembleGaslessPayload THEN returns Single`() {
        // Act
        val result = assemble(calls = listOf(mainTx), plan = null)

        // Assert
        assertThat(result).isEqualTo(
            GaslessPayload.Single(GaslessTransactionData(transaction = mainTx, fee = feeObj, nonce = nonce)),
        )
    }

    @Test
    fun `GIVEN three calls and TokenPay WHEN assembleGaslessPayload THEN returns Batch of those calls in order`() {
        // Arrange
        val calls = listOf(deployTx, approveTx, mainTx)
        val plan = GaslessFeePlan.TokenPay(feeToken = fakeToken, fee = fakeTokenFee)

        // Act
        val result = assemble(calls = calls, plan = plan)

        // Assert
        assertThat(result).isEqualTo(
            GaslessPayload.Batch(GaslessBatchTransactionData(transactions = calls, fee = feeObj, nonce = nonce)),
        )
    }

    @Test
    fun `GIVEN three calls and null plan WHEN assembleGaslessPayload THEN returns Batch of those calls in order`() {
        // Arrange
        val calls = listOf(deployTx, approveTx, mainTx)

        // Act
        val result = assemble(calls = calls, plan = null)

        // Assert
        assertThat(result).isEqualTo(
            GaslessPayload.Batch(GaslessBatchTransactionData(transactions = calls, fee = feeObj, nonce = nonce)),
        )
    }

    @Test
    fun `GIVEN one call and TokenPayWithYieldWithdraw WHEN assembleGaslessPayload THEN withdraw appended last`() {
        // Arrange
        val plan = yieldWithdrawPlan(withdrawCallData = fakeWithdrawCallData)

        // Act
        val result = assemble(calls = listOf(mainTx), plan = plan, withdrawGasLimit = withdrawGasLimit)

        // Assert
        assertThat(result).isEqualTo(
            GaslessPayload.Batch(
                GaslessBatchTransactionData(transactions = listOf(mainTx, withdrawTx), fee = feeObj, nonce = nonce),
            ),
        )
    }

    @Test
    fun `GIVEN two calls and TokenPayWithYieldWithdraw WHEN assembleGaslessPayload THEN Batch of three withdraw last`() {
        // Arrange
        val plan = yieldWithdrawPlan(withdrawCallData = fakeWithdrawCallData)

        // Act
        val result = assemble(calls = listOf(approveTx, mainTx), plan = plan, withdrawGasLimit = withdrawGasLimit)

        // Assert
        assertThat(result).isEqualTo(
            GaslessPayload.Batch(
                GaslessBatchTransactionData(
                    transactions = listOf(approveTx, mainTx, withdrawTx),
                    fee = feeObj,
                    nonce = nonce,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN NativePay WHEN assembleGaslessPayload THEN throws IllegalStateException`() {
        // Arrange
        val plan = GaslessFeePlan.NativePay(fee = fakeNativeFee)

        // Act
        val actual = runCatching { assemble(calls = listOf(mainTx), plan = plan) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN yield withdraw plan without gas limit WHEN assembleGaslessPayload THEN throws IllegalStateException`() {
        // Arrange
        val plan = yieldWithdrawPlan(withdrawCallData = fakeWithdrawCallData)

        // Act
        val actual = runCatching { assemble(calls = listOf(mainTx), plan = plan) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN no calls WHEN assembleGaslessPayload THEN throws IllegalArgumentException`() {
        // Act
        val actual = runCatching { assemble(calls = emptyList(), plan = null) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun assemble(
        calls: List<GaslessTransactionData.Transaction>,
        plan: GaslessFeePlan?,
        withdrawGasLimit: BigInteger? = null,
        approveGasLimit: BigInteger? = null,
    ) = CreateAndSendEvmGaslessTransactionUseCase.assembleGaslessPayload(
        calls = calls,
        feeObj = feeObj,
        nonce = nonce,
        plan = plan,
        withdrawGasLimit = withdrawGasLimit,
        approveGasLimit = approveGasLimit,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fee plan WHEN upgradesYieldModule THEN true only for an upgrade-wrapped yield withdraw`(
        model: UpgradePlanModel,
    ) {
        // Act
        val actual = with(CreateAndSendEvmGaslessTransactionUseCase) { model.plan.upgradesYieldModule() }

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        UpgradePlanModel(
            plan = yieldWithdrawPlan(
                withdrawCallData = EthereumYieldSupplyUpgradeToAndCallCallData(
                    newImplementation = "0xnew",
                    innerCallData = fakeWithdrawCallData,
                ),
            ),
            expected = true,
        ),
        UpgradePlanModel(plan = yieldWithdrawPlan(withdrawCallData = fakeWithdrawCallData), expected = false),
        UpgradePlanModel(plan = GaslessFeePlan.TokenPay(feeToken = fakeToken, fee = fakeTokenFee), expected = false),
        UpgradePlanModel(plan = null, expected = false),
    )

    private fun yieldWithdrawPlan(withdrawCallData: SmartContractCallData) = GaslessFeePlan.TokenPayWithYieldWithdraw(
        feeToken = fakeToken,
        fee = fakeTokenFee,
        withdrawAmount = BigInteger.ONE,
        withdrawCallData = withdrawCallData,
        yieldModuleAddress = "0xmodule",
    )

    data class UpgradePlanModel(val plan: GaslessFeePlan?, val expected: Boolean)

    @Test
    fun `GIVEN TokenPayWithApproval and two calls WHEN assembleGaslessPayload THEN Batch with approve first`() {
        // Arrange
        val approveGasLimit = BigInteger.valueOf(65_000)
        val plan = GaslessFeePlan.TokenPayWithApproval(
            feeToken = fakeToken,
            fee = fakeTokenFee,
            approval = approval(extras = EthereumTransactionExtras(callData = fakeApproveCallData)),
        )
        val planApproveTx = GaslessTransactionData.Transaction(
            to = APPROVE_TOKEN_CONTRACT,
            value = BigInteger.ZERO,
            gasLimit = approveGasLimit,
            data = fakeApproveCallData.data,
        )

        // Act
        val result = assemble(calls = listOf(deployTx, mainTx), plan = plan, approveGasLimit = approveGasLimit)

        // Assert
        assertThat(result).isEqualTo(
            GaslessPayload.Batch(
                GaslessBatchTransactionData(
                    transactions = listOf(planApproveTx, deployTx, mainTx),
                    fee = feeObj,
                    nonce = nonce,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN TokenPayWithApproval without approve call data WHEN assembleGaslessPayload THEN throws`() {
        // Arrange
        val plan = GaslessFeePlan.TokenPayWithApproval(
            feeToken = fakeToken,
            fee = fakeTokenFee,
            approval = approval(extras = null),
        )

        // Act
        val actual = runCatching {
            assemble(calls = listOf(mainTx), plan = plan, approveGasLimit = BigInteger.valueOf(65_000))
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN TokenPayWithApproval without approve gas limit WHEN assembleGaslessPayload THEN throws`() {
        // Arrange
        val plan = GaslessFeePlan.TokenPayWithApproval(
            feeToken = fakeToken,
            fee = fakeTokenFee,
            approval = approval(extras = EthereumTransactionExtras(callData = fakeApproveCallData)),
        )

        // Act
        val actual = runCatching { assemble(calls = listOf(mainTx), plan = plan) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalStateException::class.java)
    }

    private val fakeApproveCallData = object : SmartContractCallData {
        override val methodId: String = "0x095ea7b3"
        override val data: ByteArray = byteArrayOf(0x09, 0x5e)
        override fun validate(blockchain: com.tangem.blockchain.common.Blockchain) = true
    }

    private fun approval(extras: EthereumTransactionExtras?) = GaslessApproval(
        spenderAddress = "0xspender",
        approvalTransaction = TransactionData.Uncompiled(
            amount = mockk(relaxed = true),
            fee = null,
            sourceAddress = "0xsource",
            destinationAddress = APPROVE_TOKEN_CONTRACT,
            extras = extras,
        ),
    )

    private companion object {
        const val APPROVE_TOKEN_CONTRACT = "0xusdt"
    }
}