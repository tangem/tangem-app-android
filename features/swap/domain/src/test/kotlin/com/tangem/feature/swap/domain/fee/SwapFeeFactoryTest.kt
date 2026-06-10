package com.tangem.feature.swap.domain.fee

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Unit tests for [SwapFeeFactory] ([REDACTED_TASK_KEY] — Phase 3).
 *
 * Verifies the bucket → [com.tangem.blockchain.common.transaction.Fee] mapping rules used by
 * `SwapInteractorImpl.loadSwapFee` to assemble a `SwapFee` from a raw `TransactionFeeResult`.
 *
 * Golden mapping table — must match `FeeItemConverter` in send-v2:
 *
 * | TransactionFee shape   | FeeBucket    | Selected Fee                            |
 * |------------------------|--------------|-----------------------------------------|
 * | Single(normal)         | MARKET       | normal                                  |
 * | Single(normal)         | SLOW         | normal (degraded — no minimum)          |
 * | Single(normal)         | FAST         | normal (degraded — no priority)         |
 * | Choosable(min/n/p)     | SLOW         | minimum                                 |
 * | Choosable(min/n/p)     | MARKET       | normal                                  |
 * | Choosable(min/n/p)     | FAST         | priority                                |
 * | Choosable(min/n/p)     | SUGGESTED    | normal (caller overrides if applicable) |
 * | Choosable(min/n/p)     | CUSTOM       | normal (caller overrides)               |
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapFeeFactoryTest {

    private val nativeFeeTokenStatus: CryptoCurrencyStatus = mockk(relaxed = true)

    // -------------------------------------------------------------------------
    // TransactionFee.Single
    // -------------------------------------------------------------------------

    @Test
    fun `fromLoaded with Single picks the normal fee for MARKET bucket`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.MARKET,
        )

        assertThat(result.fee).isEqualTo(singleFee.normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.MARKET)
        assertThat(result.otherNativeFee).isEqualTo(BigDecimal.ZERO)
        assertThat(result.selectedFeeToken).isSameInstanceAs(nativeFeeTokenStatus)
    }

    @Test
    fun `fromLoaded with Single degrades SLOW bucket to normal fee`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.SLOW,
        )

        assertThat(result.fee).isEqualTo(singleFee.normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.SLOW)
    }

    @Test
    fun `fromLoaded with Single degrades FAST bucket to normal fee`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.FAST,
        )

        assertThat(result.fee).isEqualTo(singleFee.normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.FAST)
    }

    // -------------------------------------------------------------------------
    // TransactionFee.Choosable
    // -------------------------------------------------------------------------

    @Test
    fun `fromLoaded with Choosable picks minimum fee for SLOW bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.SLOW,
        )

        assertThat(result.fee).isEqualTo(slow)
    }

    @Test
    fun `fromLoaded with Choosable picks normal fee for MARKET bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.MARKET,
        )

        assertThat(result.fee).isEqualTo(normal)
    }

    @Test
    fun `fromLoaded with Choosable picks priority fee for FAST bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.FAST,
        )

        assertThat(result.fee).isEqualTo(fast)
    }

    @Test
    fun `fromLoaded with Choosable falls back to normal fee for SUGGESTED bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.SUGGESTED,
        )

        assertThat(result.fee).isEqualTo(normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.SUGGESTED)
    }

    @Test
    fun `fromLoaded with Choosable falls back to normal fee for CUSTOM bucket`() {
        val slow = ethLegacyFee(BigDecimal("0.001"))
        val normal = ethLegacyFee(BigDecimal("0.002"))
        val fast = ethLegacyFee(BigDecimal("0.003"))
        val choosable = TransactionFee.Choosable(minimum = slow, normal = normal, priority = fast)

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(choosable),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.CUSTOM,
        )

        assertThat(result.fee).isEqualTo(normal)
        assertThat(result.feeBucket).isEqualTo(FeeBucket.CUSTOM)
    }

    // -------------------------------------------------------------------------
    // LoadedExtended (gasless / token fee)
    // -------------------------------------------------------------------------

    @Test
    fun `fromLoadedExtended picks normal fee from transactionFeeExtended for MARKET`() {
        val rawFee = ethLegacyFee(BigDecimal("0.002"))
        val txFee = TransactionFee.Single(normal = rawFee)
        val extended = mockk<TransactionFeeExtended>(relaxed = true) {
            io.mockk.every { transactionFee } returns txFee
        }

        val result = SwapFeeFactory.fromLoadedExtended(
            transactionFeeResult = TransactionFeeResult.LoadedExtended(extended),
            selectedFeeToken = nativeFeeTokenStatus,
            feeBucket = FeeBucket.MARKET,
        )

        assertThat(result.fee).isEqualTo(rawFee)
        assertThat(result.transactionFeeResult).isInstanceOf(TransactionFeeResult.LoadedExtended::class.java)
    }

    // -------------------------------------------------------------------------
    // otherNativeFee propagation
    // -------------------------------------------------------------------------

    @Test
    fun `otherNativeFee is propagated verbatim into SwapFee`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))
        val bridgeFee = BigDecimal("0.5")

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
            otherNativeFee = bridgeFee,
        )

        assertThat(result.otherNativeFee).isEquivalentAccordingToCompareTo(bridgeFee)
    }

    @Test
    fun `default otherNativeFee is ZERO`() {
        val singleFee = TransactionFee.Single(normal = ethLegacyFee(BigDecimal("0.002")))

        val result = SwapFeeFactory.fromLoaded(
            transactionFeeResult = TransactionFeeResult.Loaded(singleFee),
            selectedFeeToken = nativeFeeTokenStatus,
        )

        assertThat(result.otherNativeFee).isEqualTo(BigDecimal.ZERO)
    }

    // -------------------------------------------------------------------------
    // from() generic dispatcher
    // -------------------------------------------------------------------------

    @Test
    fun `from dispatches Loaded to fromLoaded`() {
        val rawFee = ethLegacyFee(BigDecimal("0.002"))
        val transactionFeeResult = TransactionFeeResult.Loaded(TransactionFee.Single(normal = rawFee))

        val result = SwapFeeFactory.from(
            transactionFeeResult = transactionFeeResult,
            selectedFeeToken = nativeFeeTokenStatus,
        )

        assertThat(result.fee).isEqualTo(rawFee)
        assertThat(result.transactionFeeResult).isSameInstanceAs(transactionFeeResult)
    }

    @Test
    fun `from dispatches LoadedExtended to fromLoadedExtended`() {
        val rawFee = ethLegacyFee(BigDecimal("0.002"))
        val txFee = TransactionFee.Single(normal = rawFee)
        val extended = mockk<TransactionFeeExtended>(relaxed = true) {
            io.mockk.every { transactionFee } returns txFee
        }
        val transactionFeeResult = TransactionFeeResult.LoadedExtended(extended)

        val result = SwapFeeFactory.from(
            transactionFeeResult = transactionFeeResult,
            selectedFeeToken = nativeFeeTokenStatus,
        )

        assertThat(result.fee).isEqualTo(rawFee)
        assertThat(result.transactionFeeResult).isSameInstanceAs(transactionFeeResult)
    }

    // -------------------------------------------------------------------------
    // FeeBucket.toAnalyticsName labels
    // -------------------------------------------------------------------------

    @Test
    fun `FeeBucket toAnalyticsName returns labels compatible with legacy FeeType`() {
        // SLOW didn't exist in the legacy FeeType; new label is "Min".
        assertThat(FeeBucket.SLOW.toAnalyticsName()).isEqualTo("Min")
        // MARKET corresponds to legacy FeeType.NORMAL.getNameForAnalytics() == "Normal".
        assertThat(FeeBucket.MARKET.toAnalyticsName()).isEqualTo("Normal")
        // FAST corresponds to legacy FeeType.PRIORITY.getNameForAnalytics() == "Max".
        assertThat(FeeBucket.FAST.toAnalyticsName()).isEqualTo("Max")
        assertThat(FeeBucket.SUGGESTED.toAnalyticsName()).isEqualTo("Suggested")
        assertThat(FeeBucket.CUSTOM.toAnalyticsName()).isEqualTo("Custom")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun ethLegacyFee(value: BigDecimal): Fee.Ethereum.Legacy = Fee.Ethereum.Legacy(
        amount = Amount(currencySymbol = "ETH", value = value, decimals = 18),
        gasLimit = BigInteger.valueOf(100_000),
        gasPrice = BigInteger.valueOf(20_000_000_000),
    )
}