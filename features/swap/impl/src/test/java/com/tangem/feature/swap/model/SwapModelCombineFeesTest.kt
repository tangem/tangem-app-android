package com.tangem.feature.swap.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.feature.swap.domain.models.ui.IntegratedApprovalData
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for [SwapModel]'s integrated approve+swap fee combination (private `combineTransactionFees`
 * / `sumEvmFees`). These are pure functions, so they are exercised via reflection (the public
 * fee-loading pipeline that calls them requires a large amount of async wiring; the plan permits
 * reflection for pure private functions where the public path is brittle).
 *
 * Verifies:
 *  - Choosable + Choosable → per-bucket [TransactionFee.Choosable] with summed amount + gasLimit.
 *  - Single involved (either side) → [TransactionFee.Single] summing the `normal` fees.
 *  - Legacy EVM fees are summed too (amount + gasLimit).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelCombineFeesTest : SwapModelTestBase() {

    private lateinit var model: SwapModel

    @BeforeEach
    fun setUp() {
        setUpBase()
        model = createModel()
    }

    @Test
    fun `GIVEN Choosable plus Choosable THEN per-bucket sum of amount and gasLimit`() {
        val approval = choosable(min = 1, normal = 2, priority = 3, gas = 21_000)
        val swap = choosable(min = 10, normal = 20, priority = 30, gas = 50_000)

        val result = combineTransactionFees(approval, swap)

        assertThat(result).isInstanceOf(TransactionFee.Choosable::class.java)
        val choosable = result as TransactionFee.Choosable
        assertEip1559(choosable.minimum, expectedValue = 11, expectedGas = 71_000)
        assertEip1559(choosable.normal, expectedValue = 22, expectedGas = 71_000)
        assertEip1559(choosable.priority, expectedValue = 33, expectedGas = 71_000)
    }

    @Test
    fun `GIVEN Single approval and Choosable swap THEN result is Single summing normals`() {
        val approval = TransactionFee.Single(normal = eip1559(value = 2, gas = 21_000))
        val swap = choosable(min = 10, normal = 20, priority = 30, gas = 50_000)

        val result = combineTransactionFees(approval, swap)

        assertThat(result).isInstanceOf(TransactionFee.Single::class.java)
        assertEip1559((result as TransactionFee.Single).normal, expectedValue = 22, expectedGas = 71_000)
    }

    @Test
    fun `GIVEN both Single THEN result is Single summing normals`() {
        val approval = TransactionFee.Single(normal = eip1559(value = 5, gas = 21_000))
        val swap = TransactionFee.Single(normal = eip1559(value = 7, gas = 30_000))

        val result = combineTransactionFees(approval, swap)

        assertThat(result).isInstanceOf(TransactionFee.Single::class.java)
        assertEip1559((result as TransactionFee.Single).normal, expectedValue = 12, expectedGas = 51_000)
    }

    @Test
    fun `GIVEN Legacy EVM fees THEN summed amount and gasLimit`() {
        val approval = TransactionFee.Single(normal = legacy(value = 2, gas = 21_000))
        val swap = TransactionFee.Single(normal = legacy(value = 20, gas = 50_000))

        val result = combineTransactionFees(approval, swap)

        val normal = (result as TransactionFee.Single).normal
        assertThat(normal).isInstanceOf(Fee.Ethereum.Legacy::class.java)
        val legacy = normal as Fee.Ethereum.Legacy
        assertThat(legacy.amount.value).isEqualTo(BigDecimal(22))
        assertThat(legacy.gasLimit).isEqualTo(BigInteger.valueOf(71_000))
    }

    @Test
    fun `loadAndStoreIntegratedApproval stores IntegratedApprovalData on the quote state and returns combined fee`() =
        runTest {
            val provider = swapProvider()
            val quoteState = quotesLoadedState(
                provider = provider,
                permissionState = permissionSettings(type = ApproveType.UNLIMITED, spender = "0xSpender"),
            )
            model.dataState = model.dataState.copy(
                selectedProvider = provider,
                lastLoadedSwapStates = mapOf(provider to quoteState),
            )
            val approvalData = IntegratedApprovalData(
                approvalTransaction = mockk<TransactionData.Uncompiled>(relaxed = true),
                approvalFee = TransactionFee.Single(normal = eip1559(value = 2, gas = 21_000)),
                approveType = ApproveType.UNLIMITED,
            )
            coEvery {
                swapInteractor.loadIntegratedApprovalData(
                    fromStatus = any(),
                    spenderAddress = any(),
                    approveType = any(),
                    approvalAmount = any(),
                )
            } returns approvalData.right()

            val combined = loadAndStoreIntegratedApproval(
                fromSwapCurrencyStatus = swapCurrencyStatus(),
                quoteState = quoteState,
                permissionSettings = permissionSettings(
                    type = ApproveType.UNLIMITED,
                    spender = "0xSpender",
                ),
                approvalAmount = BigDecimal.ONE,
                swapTxFee = TransactionFee.Single(normal = eip1559(value = 20, gas = 50_000)),
            )

            assertThat(combined.isRight()).isTrue()
            combined.onRight { fee ->
                assertEip1559((fee as TransactionFee.Single).normal, expectedValue = 22, expectedGas = 71_000)
            }
            // Stored on the current loaded state for later submission.
            val stored = (model.dataState.lastLoadedSwapStates[provider] as SwapState.QuotesLoadedState)
                .integratedApprovalData
            assertThat(stored).isEqualTo(approvalData)
        }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private suspend fun loadAndStoreIntegratedApproval(
        fromSwapCurrencyStatus: com.tangem.domain.swap.models.SwapCurrencyStatus,
        quoteState: SwapState.QuotesLoadedState,
        permissionSettings: PermissionDataState.PermissionSettings,
        approvalAmount: BigDecimal,
        swapTxFee: TransactionFee,
    ): arrow.core.Either<com.tangem.domain.transaction.error.GetFeeError, TransactionFee> {
        val method = SwapModel::class.java.declaredMethods.first { it.name == "loadAndStoreIntegratedApproval" }
            .apply { isAccessible = true }
        return invokeSuspend(method, fromSwapCurrencyStatus, quoteState, permissionSettings, approvalAmount, swapTxFee)
            as arrow.core.Either<com.tangem.domain.transaction.error.GetFeeError, TransactionFee>
    }

    private suspend fun invokeSuspend(method: java.lang.reflect.Method, vararg args: Any?): Any? =
        kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn { cont ->
            method.invoke(model, *args, cont)
        }

    private fun combineTransactionFees(approvalFee: TransactionFee, swapFee: TransactionFee): TransactionFee {
        val method = SwapModel::class.java.getDeclaredMethod(
            "combineTransactionFees",
            TransactionFee::class.java,
            TransactionFee::class.java,
        ).apply { isAccessible = true }
        return method.invoke(model, approvalFee, swapFee) as TransactionFee
    }

    private fun choosable(min: Int, normal: Int, priority: Int, gas: Long): TransactionFee.Choosable =
        TransactionFee.Choosable(
            minimum = eip1559(value = min, gas = gas),
            normal = eip1559(value = normal, gas = gas),
            priority = eip1559(value = priority, gas = gas),
        )

    private fun eip1559(value: Int, gas: Long): Fee.Ethereum.EIP1559 = Fee.Ethereum.EIP1559(
        amount = ethAmount(value),
        gasLimit = BigInteger.valueOf(gas),
        maxFeePerGas = BigInteger.ONE,
        priorityFee = BigInteger.ONE,
    )

    private fun legacy(value: Int, gas: Long): Fee.Ethereum.Legacy = Fee.Ethereum.Legacy(
        amount = ethAmount(value),
        gasLimit = BigInteger.valueOf(gas),
        gasPrice = BigInteger.ONE,
    )

    private fun ethAmount(value: Int): Amount = Amount(
        currencySymbol = "ETH",
        value = BigDecimal(value),
        decimals = 18,
    )

    private fun assertEip1559(fee: Fee, expectedValue: Int, expectedGas: Long) {
        assertThat(fee).isInstanceOf(Fee.Ethereum.EIP1559::class.java)
        val eip = fee as Fee.Ethereum.EIP1559
        assertThat(eip.amount.value).isEqualTo(BigDecimal(expectedValue))
        assertThat(eip.gasLimit).isEqualTo(BigInteger.valueOf(expectedGas))
    }
}