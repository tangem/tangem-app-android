package com.tangem.feature.swap.domain.fee

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Pure-JVM unit tests for [com.tangem.feature.swap.domain.fee.PatchEthGasLimitForSwap] ([REDACTED_TASK_KEY]).
 *
 * Pinned behavior — these tests guard the gas-bump arithmetic against accidental drift in:
 *  - Ethereum [com.tangem.blockchain.common.transaction.Fee.Ethereum.Legacy] / [com.tangem.blockchain.common.transaction.Fee.Ethereum.EIP1559]: gasLimit *= percentage / 100,
 *    amount = (newGasLimit * gasPrice) shifted left by amount decimals, decimals preserved.
 *  - [com.tangem.blockchain.common.transaction.Fee.Ethereum.TokenCurrency]: throws (current `error("handle in [REDACTED_TASK_KEY]")`).
 *  - All non-Ethereum [com.tangem.blockchain.common.transaction.Fee] subtypes: returned unchanged.
 *  - [com.tangem.blockchain.common.transaction.TransactionFee.Choosable]: applies the bump to all three legs (minimum/normal/priority).
 *  - [com.tangem.blockchain.common.transaction.TransactionFee.Single]: applies the bump to `normal`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PatchEthGasLimitForSwapTest {

    private val dexBump = PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.DEX_PERCENTAGE)
    private val sendBump = PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.SEND_PERCENTAGE)

    // region Ethereum.Legacy

    @Test
    fun `GIVEN Ethereum Legacy fee WHEN dex bump applied THEN gasLimit is multiplied by 112 percent`() {
        // amount = gasLimit * gasPrice shifted left by 18 → 100000 * 20_000_000_000 / 1e18 = 0.000002 ETH
        val gasLimit = BigInteger.valueOf(100_000)
        val gasPrice = BigInteger.valueOf(20_000_000_000) // 20 gwei
        val amountValue = BigDecimal("0.000002") // 100_000 * 20e9 / 1e18
        val initialFee = Fee.Ethereum.Legacy(
            amount = ethAmount(amountValue, decimals = 18),
            gasLimit = gasLimit,
            gasPrice = gasPrice,
        )

        val result = dexBump(TransactionFee.Single(normal = initialFee))

        val patched = (result as TransactionFee.Single).normal as Fee.Ethereum.Legacy
        // 100_000 * 112 / 100 = 112_000
        assertThat(patched.gasLimit).isEqualTo(BigInteger.valueOf(112_000))
        // amount = 112_000 * 20_000_000_000 / 1e18 = 0.00000224
        assertThat(patched.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00000224"))
        // amount decimals must be preserved
        assertThat(patched.amount.decimals).isEqualTo(18)
        // gasPrice unchanged
        assertThat(patched.gasPrice).isEqualTo(gasPrice)
    }

    @Test
    fun `GIVEN Ethereum Legacy fee WHEN send bump applied THEN gasLimit is multiplied by 105 percent`() {
        val gasLimit = BigInteger.valueOf(100_000)
        val gasPrice = BigInteger.valueOf(20_000_000_000)
        val initialFee = Fee.Ethereum.Legacy(
            amount = ethAmount(BigDecimal("0.000002"), decimals = 18),
            gasLimit = gasLimit,
            gasPrice = gasPrice,
        )

        val result = sendBump(TransactionFee.Single(normal = initialFee))

        val patched = (result as TransactionFee.Single).normal as Fee.Ethereum.Legacy
        // 100_000 * 105 / 100 = 105_000
        assertThat(patched.gasLimit).isEqualTo(BigInteger.valueOf(105_000))
        // amount = 105_000 * 20_000_000_000 / 1e18 = 0.0000021
        assertThat(patched.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.0000021"))
        assertThat(patched.amount.decimals).isEqualTo(18)
        assertThat(patched.gasPrice).isEqualTo(gasPrice)
    }

    // endregion

    // region Ethereum.EIP1559

    @Test
    fun `GIVEN Ethereum EIP1559 fee WHEN dex bump applied THEN gasLimit and amount are bumped`() {
        val gasLimit = BigInteger.valueOf(50_000)
        // Pretend gasPrice (effective) is 30 gwei → amount = 50_000 * 30e9 / 1e18 = 0.0000015
        val initialFee = Fee.Ethereum.EIP1559(
            amount = ethAmount(BigDecimal("0.0000015"), decimals = 18),
            gasLimit = gasLimit,
            maxFeePerGas = BigInteger.valueOf(40_000_000_000),
            priorityFee = BigInteger.valueOf(2_000_000_000),
        )

        val result = dexBump(TransactionFee.Single(normal = initialFee))

        val patched = (result as TransactionFee.Single).normal as Fee.Ethereum.EIP1559
        // 50_000 * 112 / 100 = 56_000
        assertThat(patched.gasLimit).isEqualTo(BigInteger.valueOf(56_000))
        // amount = 56_000 * 30e9 / 1e18 = 0.00000168
        assertThat(patched.amount.value).isEquivalentAccordingToCompareTo(BigDecimal("0.00000168"))
        assertThat(patched.amount.decimals).isEqualTo(18)
        // EIP1559-specific fields unchanged
        assertThat(patched.maxFeePerGas).isEqualTo(BigInteger.valueOf(40_000_000_000))
        assertThat(patched.priorityFee).isEqualTo(BigInteger.valueOf(2_000_000_000))
    }

    // endregion

    // region Ethereum.TokenCurrency throws

    @Test
    fun `GIVEN Ethereum TokenCurrency fee WHEN dex bump applied THEN throws IllegalStateException`() {
        val tokenFee = Fee.Ethereum.TokenCurrency(
            amount = ethAmount(BigDecimal("0.001"), decimals = 18),
            gasLimit = BigInteger.valueOf(100_000),
            coinPriceInToken = BigInteger.ONE,
            feeTransferGasLimit = BigInteger.valueOf(50_000),
            baseGas = BigInteger.valueOf(21_000),
        )

        assertThrows<IllegalStateException> {
            dexBump(TransactionFee.Single(normal = tokenFee))
        }
    }

    @Test
    fun `GIVEN Ethereum TokenCurrency fee WHEN dex bump applied THEN error message points to [REDACTED_TASK_KEY]`() {
        val tokenFee = Fee.Ethereum.TokenCurrency(
            amount = ethAmount(BigDecimal("0.001"), decimals = 18),
            gasLimit = BigInteger.valueOf(100_000),
            coinPriceInToken = BigInteger.ONE,
            feeTransferGasLimit = BigInteger.valueOf(50_000),
            baseGas = BigInteger.valueOf(21_000),
        )

        val thrown = runCatching { dexBump(TransactionFee.Single(normal = tokenFee)) }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown?.message).contains("[REDACTED_TASK_KEY]")
    }

    // endregion

    // region Non-Ethereum subtypes returned unchanged

    @Test
    fun `GIVEN Common fee WHEN dex bump applied THEN fee is unchanged`() {
        val initial = Fee.Common(amount = ethAmount(BigDecimal("0.001"), decimals = 8))
        val result = dexBump(TransactionFee.Single(normal = initial))
        assertThat((result as TransactionFee.Single).normal).isSameInstanceAs(initial)
    }

    @Test
    fun `GIVEN Bitcoin fee WHEN dex bump applied THEN fee is unchanged`() {
        val initial = Fee.Bitcoin(
            amount = ethAmount(BigDecimal("0.0001"), decimals = 8),
            satoshiPerByte = BigDecimal("10"),
            txSize = BigDecimal("250"),
        )
        val result = dexBump(TransactionFee.Single(normal = initial))
        assertThat((result as TransactionFee.Single).normal).isSameInstanceAs(initial)
    }

    @Test
    fun `GIVEN Tron fee WHEN dex bump applied THEN fee is unchanged`() {
        val initial = Fee.Tron(
            amount = ethAmount(BigDecimal("0.5"), decimals = 6),
            remainingEnergy = 1000L,
            feeEnergy = 100L,
        )
        val result = dexBump(TransactionFee.Single(normal = initial))
        assertThat((result as TransactionFee.Single).normal).isSameInstanceAs(initial)
    }

    @Test
    fun `GIVEN Sui fee WHEN dex bump applied THEN fee is unchanged`() {
        val initial = Fee.Sui(
            amount = ethAmount(BigDecimal("0.0001"), decimals = 9),
            gasBudget = 10_000L,
            gasPrice = 1_000L,
        )
        val result = dexBump(TransactionFee.Single(normal = initial))
        assertThat((result as TransactionFee.Single).normal).isSameInstanceAs(initial)
    }

    @Test
    fun `GIVEN Aptos fee WHEN dex bump applied THEN fee is unchanged`() {
        val initial = Fee.Aptos(
            amount = ethAmount(BigDecimal("0.0001"), decimals = 8),
            gasUnitPrice = 100L,
            gasLimit = 10_000L,
        )
        val result = dexBump(TransactionFee.Single(normal = initial))
        assertThat((result as TransactionFee.Single).normal).isSameInstanceAs(initial)
    }

    @Test
    fun `GIVEN Hedera fee WHEN dex bump applied THEN fee is unchanged`() {
        val initial = Fee.Hedera(
            amount = ethAmount(BigDecimal("0.001"), decimals = 8),
            additionalHBARFee = BigDecimal.ZERO,
        )
        val result = dexBump(TransactionFee.Single(normal = initial))
        assertThat((result as TransactionFee.Single).normal).isSameInstanceAs(initial)
    }

    // endregion

    // region TransactionFee.Choosable bumps all three legs

    @Test
    fun `GIVEN Choosable fee with three Ethereum Legacy legs WHEN dex bump applied THEN every leg is bumped`() {
        val legacyMin = Fee.Ethereum.Legacy(
            amount = ethAmount(BigDecimal("0.000001"), decimals = 18),
            gasLimit = BigInteger.valueOf(50_000),
            gasPrice = BigInteger.valueOf(20_000_000_000),
        )
        val legacyNormal = Fee.Ethereum.Legacy(
            amount = ethAmount(BigDecimal("0.000002"), decimals = 18),
            gasLimit = BigInteger.valueOf(100_000),
            gasPrice = BigInteger.valueOf(20_000_000_000),
        )
        val legacyPriority = Fee.Ethereum.Legacy(
            amount = ethAmount(BigDecimal("0.000003"), decimals = 18),
            gasLimit = BigInteger.valueOf(150_000),
            gasPrice = BigInteger.valueOf(20_000_000_000),
        )

        val result = dexBump(
            TransactionFee.Choosable(
                minimum = legacyMin,
                normal = legacyNormal,
                priority = legacyPriority,
            ),
        ) as TransactionFee.Choosable

        assertThat((result.minimum as Fee.Ethereum.Legacy).gasLimit).isEqualTo(BigInteger.valueOf(56_000))
        assertThat((result.normal as Fee.Ethereum.Legacy).gasLimit).isEqualTo(BigInteger.valueOf(112_000))
        assertThat((result.priority as Fee.Ethereum.Legacy).gasLimit).isEqualTo(BigInteger.valueOf(168_000))
    }

    @Test
    fun `GIVEN Choosable fee with mixed legs WHEN bump applied THEN only Ethereum legs are bumped`() {
        // Two Ethereum legs and one Common leg → only the Ethereum ones are scaled.
        val ethMin = Fee.Ethereum.Legacy(
            amount = ethAmount(BigDecimal("0.000001"), decimals = 18),
            gasLimit = BigInteger.valueOf(50_000),
            gasPrice = BigInteger.valueOf(10_000_000_000),
        )
        val commonNormal = Fee.Common(amount = ethAmount(BigDecimal("0.5"), decimals = 8))
        val ethPriority = Fee.Ethereum.Legacy(
            amount = ethAmount(BigDecimal("0.000003"), decimals = 18),
            gasLimit = BigInteger.valueOf(150_000),
            gasPrice = BigInteger.valueOf(10_000_000_000),
        )

        val result = sendBump(
            TransactionFee.Choosable(
                minimum = ethMin,
                normal = commonNormal,
                priority = ethPriority,
            ),
        ) as TransactionFee.Choosable

        assertThat((result.minimum as Fee.Ethereum.Legacy).gasLimit).isEqualTo(BigInteger.valueOf(52_500))
        assertThat(result.normal).isSameInstanceAs(commonNormal)
        assertThat((result.priority as Fee.Ethereum.Legacy).gasLimit).isEqualTo(BigInteger.valueOf(157_500))
    }

    // endregion

    // region Decimals preserved for non-18 decimals

    @Test
    fun `GIVEN Ethereum Legacy fee with 9 decimals WHEN dex bump applied THEN amount decimals are preserved`() {
        val gasLimit = BigInteger.valueOf(21_000)
        val gasPrice = BigInteger.valueOf(1_000_000) // 1 gwei in 9-decimal native units
        // amount = 21_000 * 1_000_000 / 1e9 = 0.021
        val initial = Fee.Ethereum.Legacy(
            amount = ethAmount(BigDecimal("0.021"), decimals = 9),
            gasLimit = gasLimit,
            gasPrice = gasPrice,
        )

        val result = dexBump(TransactionFee.Single(normal = initial))

        val patched = (result as TransactionFee.Single).normal as Fee.Ethereum.Legacy
        assertThat(patched.amount.decimals).isEqualTo(9)
        assertThat(patched.gasLimit).isEqualTo(BigInteger.valueOf(23_520)) // 21_000 * 112 / 100
    }

    // endregion

    private fun ethAmount(value: BigDecimal, decimals: Int): Amount = Amount(
        currencySymbol = "ETH",
        value = value,
        decimals = decimals,
    )
}